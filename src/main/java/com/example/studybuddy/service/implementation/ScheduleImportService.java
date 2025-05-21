package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.ManualScheduleDTO;
import com.example.studybuddy.repository.dto.ScheduleImportRequestDTO;
import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.User;
import com.opencsv.CSVReader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import com.example.studybuddy.utils.GoogleSheetFetcher;


import java.io.InputStreamReader;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScheduleImportService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final GoogleSheetFetcher googleSheetFetcher;

    public ScheduleImportService(EventRepository eventRepository, UserRepository userRepository, GoogleSheetFetcher googleSheetFetcher) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.googleSheetFetcher = googleSheetFetcher;
    }


    private int findGroupColumnIndex(String[] headerRow, String groupName) {
        for (int i = 0; i < headerRow.length; i++) {
            if (headerRow[i] != null && headerRow[i].trim().contains(groupName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Group column not found: " + groupName);
    }


    private void createReccuringEvents(Map<DayOfWeek, List<ScheduleEntry>> scheduleMap, ScheduleImportRequestDTO request, User user) {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = request.getSemesterEndDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek ziua = date.getDayOfWeek();

            if (!scheduleMap.containsKey(ziua)) continue;

            for(ScheduleEntry e : scheduleMap.get(ziua)) {
                LocalDateTime start = LocalDateTime.of(date, e.start);
                LocalDateTime end = LocalDateTime.of(date, e.end);

                Event event = new Event();
                event.setTitle(e.title);
                event.setStartTime(start);
                event.setEndTime(end);
                event.setUser(user);
                event.setImported(true);
                event.setScheduleLabel(request.getScheduleLabel());

                ParsedEntry p = parseEntry(e.title);
                if(p != null) {
                    StringBuilder desc = new StringBuilder();
                    if(!p.room.isEmpty()) desc.append("Sala ").append(p.room);
                    if(!p.teacher.isEmpty()) {
                        if(desc.length() > 0) desc.append(", ");
                        desc.append("cu ").append(p.teacher);
                    }
                    event.setDescription(desc.toString());
                } else {
                    event.setDescription("Importat din orar");
                }
                eventRepository.save(event);
        }
    }
}

    private ParsedEntry parseEntry(String rawContent) {
        if(rawContent == null || rawContent.isBlank()) return null;

        String room = "";
        String teacher = "";

        Matcher roomMatcher = Pattern.compile("\\b([A-Z]{1,4}\\d+[a-zA-Z]?\\b)").matcher(rawContent);
        if(roomMatcher.find()) {
            room = roomMatcher.group(1);
        }

        Matcher teacherMatcher = Pattern.compile("([A-Z]\\.\\s?[A-Z][a-zăîâșț]+)").matcher(rawContent);
        if(teacherMatcher.find()) {
            teacher = teacherMatcher.group(1);
        }

        return new ParsedEntry(room, teacher);
    }


    private String normalizeGroupName(String value) {
        return value.trim().toLowerCase().replace(".", "").replace("_", "");
    }

    private String extractSpreadSheetId(String url) {
        Pattern pattern = Pattern.compile("/d/e/([a-zA-Z0-9-_]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Spreadsheet id not found: Invalid Google Sheeys URL format");
        }
    }

    private Map<DayOfWeek, List<ScheduleEntry>> parseSchedule(List<String[]> rows, int colIndex) {
        Map<DayOfWeek, List<ScheduleEntry>> scheduleMap = new HashMap<>();
        DayOfWeek currentDay = null;

        for(String[] row : rows) {
            if(row.length <= colIndex) continue;

            String first = row[0] != null ? row[0].trim().toLowerCase() : "";
            switch (first) {
                case "l": currentDay = DayOfWeek.MONDAY; break;
                case "m": currentDay = DayOfWeek.TUESDAY; break;
                case "mi": currentDay = DayOfWeek.WEDNESDAY; break;
                case "j": currentDay = DayOfWeek.THURSDAY; break;
                case "v": currentDay = DayOfWeek.FRIDAY; break;
                case "s": currentDay = DayOfWeek.SATURDAY; break;
                case "d": currentDay = DayOfWeek.SUNDAY; break;
            }

            String timeCell = null;
            for(int c = 0; c < colIndex; c++) {
                if(row[c] != null && row[c].trim().matches("\\d{2}-\\d{2}")) {
                    timeCell = row[c].trim();
                    break;
                }
            }

            if(currentDay != null && timeCell != null) {
                String content = row[colIndex];
                if(content != null && !content.isBlank()) {
                    String[] parts = timeCell.split("-");
                    LocalTime start = LocalTime.of(Integer.parseInt(parts[0]), 0);
                    LocalTime end = LocalTime.of(Integer.parseInt(parts[1]), 0);

                    ScheduleEntry entry = new ScheduleEntry(content.trim(), start, end);
                    scheduleMap
                            .computeIfAbsent(currentDay, k -> new ArrayList<>())
                            .add(entry);
                }
            }
        }
        return scheduleMap;
    }


    public void importScheduleFromGoogleSheets(ScheduleImportRequestDTO request, HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);

            if (session == null || session.getAttribute("user") == null) {
                throw new RuntimeException("No user is logged in");
            }

            String username = session.getAttribute("user").toString();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            Document tableDoc = googleSheetFetcher.fetchSheetAsHtml(
                    request.getSheetUrl(), request.getSheetName()
            );


            List<String[]> allRows = new ArrayList<>();
            Elements rows = tableDoc.select("tr");

            for (Element row : rows) {
                Elements cells = row.select("th, td");
                String[] cols = cells.stream()
                        .map(Element::text)
                        .toArray(String[]::new);
                allRows.add(cols);
            }

            String[] header = allRows.get(0);
            int groupCol = findGroupColumnIndex(header, request.getGroup());

            List<String[]> sectionRows = allRows.subList(1, allRows.size());
            Map<DayOfWeek, List<ScheduleEntry>> scheduleMap = parseSchedule(sectionRows, groupCol);

            createReccuringEvents(scheduleMap, request, user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import schedule" + e.getMessage(), e);
        }
    }

    public void importManualSchedule(ManualScheduleDTO dto, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            throw new RuntimeException("No user is logged in");
        }

        String username = session.getAttribute("user").toString();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));

        LocalDate today = LocalDate.now();
        DayOfWeek targetDow = dto.getDayOfWeek();
        LocalDate firstDate = dto.getStartDate();
        while(firstDate.getDayOfWeek() != targetDow) {
            firstDate = firstDate.plusDays(1);
        }

        LocalDate endDate = dto.getRepeatUntil();
        for(LocalDate date = firstDate; !date.isAfter(endDate); date = date.plusWeeks(1)) {
            LocalDateTime start = LocalDateTime.of(date, dto.getStartTime());
            LocalDateTime end = LocalDateTime.of(date, dto.getEndTime());

            Event ev = new Event();
            ev.setTitle(dto.getTitle());
            ev.setStartTime(start);
            ev.setEndTime(end);
            ev.setUser(user);
            ev.setImported(true);
            ev.setScheduleLabel(dto.getScheduleLabel());
            ev.setDescription(dto.getDescription());

            eventRepository.save(ev);
        }

    }

    private static class ParsedEntry {
        String room;
        String teacher;

        public ParsedEntry(String room, String teacher) {
            this.room = room;
            this.teacher = teacher;
        }
    }

    private static class ScheduleEntry {
        String title;
        LocalTime start;
        LocalTime end;

        public ScheduleEntry(String title, LocalTime start, LocalTime end) {
            this.title = title;
            this.start = start;
            this.end = end;
        }
    }


}
