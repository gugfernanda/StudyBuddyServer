package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.ScheduleImportRequestDTO;
import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.User;
import com.opencsv.CSVReader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import com.example.studybuddy.utils.GoogleSheetUtil;


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

    public ScheduleImportService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }


    private int findGroupColumnIndex(String[] headerRow, String groupName) {
        for (int i = 0; i < headerRow.length; i++) {
            if(headerRow[i] != null && headerRow[i].trim().contains(groupName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Group column not found: " + groupName);
    }

    private Map<DayOfWeek, List<ScheduleEntry>> parseSchedule(List<String[]> rows, int colIndex) {
        Map<DayOfWeek, List<ScheduleEntry>> scheduleMap = new HashMap<>();

        DayOfWeek currentDay = null;

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);

            System.out.println("🧠 Parsing row: " + Arrays.toString(row));


            if (row.length <= colIndex) continue;


            String ora = null;

            for (int c = 0; c < row.length; c++) {
                String cell = row[c];
                if (cell != null && cell.trim().matches("\\d{2}-\\d{2}")) {
                    ora = cell.trim();
                    break;
                }
            }

            if (ora != null && currentDay != null) {
                String content = row[colIndex];
                if (content == null || content.isBlank()) continue;

                try {
                    String[] parts = ora.split("-");
                    LocalTime start = LocalTime.of(Integer.parseInt(parts[0]), 0);
                    LocalTime end = LocalTime.of(Integer.parseInt(parts[1]), 0);

                    ScheduleEntry entry = new ScheduleEntry(content.trim(), start, end);
                    scheduleMap.computeIfAbsent(currentDay, k -> new ArrayList<>()).add(entry);

                    System.out.println(" Event adugat: " + start + " - " + end + " -> " + entry.title + " @ " + currentDay);
                } catch (Exception e) {
                    System.out.println(" Eroare la parsare ora: " + ora + " -> " + e.getMessage());
                }
            }

            String cellLower = row[0].trim().toLowerCase();
            if (cellLower.length() == 1) {
                switch (cellLower) {
                    case "l": currentDay = DayOfWeek.MONDAY; break;
                    case "m": currentDay = DayOfWeek.TUESDAY; break;
                    case "w": case "mi": currentDay = DayOfWeek.WEDNESDAY; break;
                    case "j": currentDay = DayOfWeek.THURSDAY; break;
                    case "v": currentDay = DayOfWeek.FRIDAY; break;
                    case "s": currentDay = DayOfWeek.SATURDAY; break;
                    case "d": currentDay = DayOfWeek.SUNDAY; break;
                }
                System.out.println(" Zi detectata: " + currentDay);
            }

        }

        System.out.println("#################Mapă finală: " + scheduleMap.keySet());
        return scheduleMap;
    }



    private void createReccuringEvents(Map<DayOfWeek, List<ScheduleEntry>> scheduleMap, ScheduleImportRequestDTO request, User user) {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = request.getSemesterEndDate();

        for(LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek ziua = date.getDayOfWeek();

            if(scheduleMap.containsKey(ziua)) {
                for(ScheduleEntry entry : scheduleMap.get(ziua)) {
                    LocalDateTime start = LocalDateTime.of(date, entry.start);
                    LocalDateTime end = LocalDateTime.of(date, entry.end);

                    ParsedEntry parsed = parseEntry(entry.title);

                    Event event = new Event();
                    event.setTitle(entry.title);

                    if(parsed != null) {
                        StringBuilder desc = new StringBuilder();
                        if(!parsed.room.isEmpty()) {
                            desc.append("Sala ").append(parsed.room);
                        }
                        if(!parsed.teacher.isEmpty()) {
                            if(!desc.isEmpty())
                                desc.append(", ");
                            desc.append("cu ").append(parsed.teacher);
                        }
                        event.setDescription(desc.toString());
                    } else {
                        event.setDescription("Importat din orar");
                    }


                    event.setStartTime(start);
                    event.setEndTime(end);
                    event.setUser(user);
                    event.setImported(true);
                    event.setScheduleLabel(request.getScheduleLabel());


                    eventRepository.save(event);

                    System.out.println("🔽 Salvăm eveniment:");
                    System.out.println("Titlu: " + event.getTitle());
                    System.out.println("Descriere: " + event.getDescription());
                    System.out.println("Start: " + event.getStartTime());
                    System.out.println("End: " + event.getEndTime());
                    System.out.println("User: " + event.getUser().getUsername());
                    System.out.println("Label: " + event.getScheduleLabel());


                }
            }
        }
    }

    private ParsedEntry parseEntry(String rawContent) {
        if(rawContent == null || rawContent.isBlank()) return null;

        String room = "";
        String teacher = "";
        String content = rawContent.trim();

        Matcher roomMatcher = Pattern.compile("\\b([A-Z]{1,4}\\d+[a-zA-Z]?\\b)").matcher(content);
        if(roomMatcher.find()) {
            room = roomMatcher.group(1);
        }

        Matcher teacherMatcher = Pattern.compile("([A-Z]\\.\\s?[A-Z][a-zăîâșț]+)").matcher(content);
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


    public void importScheduleFromGoogleSheets(ScheduleImportRequestDTO request, HttpServletRequest httpRequest) {
        try {
            // DEBUG:
            GoogleSheetUtil.printAllSheetTabs(request.getSheetUrl());
            HttpSession session = httpRequest.getSession(false);

            if(session == null || session.getAttribute("user") == null) {
                throw new RuntimeException("No user is logged in");
            }

            String username = session.getAttribute("user").toString();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found" + username));

            String gid = GoogleSheetUtil.getGidFromSheetName(request.getSheetUrl(), request.getSheetName());

            String spreadsheetId = extractSpreadSheetId(request.getSheetUrl());
            String csvUrl = "https://docs.google.com/spreadsheets/d/e/" + spreadsheetId + "/gviz/tq?tqx=out:csv&gid=" + gid;

            //String csvUrl = request.getSheetUrl().replace("/pubhtml", "/pub?output=csv");

            List<String[]> allRows;
            try(CSVReader reader = new CSVReader(new InputStreamReader(new URL(csvUrl).openStream()))) {
                allRows = reader.readAll();
            }

            System.out.println("📥 CSV rows:");
            for (String[] row : allRows) {
                System.out.println(Arrays.toString(row));
            }

            int groupColIndex = -1;

            for (String[] row : allRows) {
                for (int col = 0; col < row.length; col++) {
                    String cell = row[col];
                    if (cell != null && normalizeGroupName(cell).equals(normalizeGroupName(request.getGroup()))) {
                        System.out.println("✅ Găsit grup: " + cell + " la colIndex: " + col);
                        groupColIndex = col;
                        break;
                    } else {
                        System.out.println("❌ Nu e grupul: " + cell + " vs " + request.getGroup());
                    }
                }

                if (groupColIndex != -1) break;
            }

            if (groupColIndex == -1) {
                throw new RuntimeException("Could not find correct group column");
            }

            List<String[]> sectionRows = allRows.subList(2, allRows.size());

            System.out.println("📚 Trimitem spre parsare " + sectionRows.size() + " rânduri:");
            for (String[] row : sectionRows) {
                System.out.println(Arrays.toString(row));
            }

            Map<DayOfWeek, List<ScheduleEntry>> parsed = parseSchedule(sectionRows, groupColIndex);

            System.out.println("🗺️ Rezultat final parse: ");
            for (Map.Entry<DayOfWeek, List<ScheduleEntry>> entry : parsed.entrySet()) {
                System.out.println("Zi: " + entry.getKey());
                for (ScheduleEntry e : entry.getValue()) {
                    System.out.println("  - " + e.start + " - " + e.end + ": " + e.title);
                }
            }

            createReccuringEvents(parsed, request, user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import schedule: " + e.getMessage(), e);
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
