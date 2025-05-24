package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.*;
import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.example.studybuddy.utils.GoogleSheetFetcher;
import org.springframework.util.StringUtils;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class ScheduleImportService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final GoogleSheetFetcher googleSheetFetcher;
    private final ObjectMapper objectMapper;


    public ScheduleImportService(EventRepository eventRepository, UserRepository userRepository, GoogleSheetFetcher googleSheetFetcher, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.googleSheetFetcher = googleSheetFetcher;
        this.objectMapper = objectMapper;
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

    @Transactional
    public void deleteScheduleByLabel(String label, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            throw new RuntimeException("No user is logged in");
        }

        String username = session.getAttribute("user").toString();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));

        eventRepository.deleteAllByUserAndScheduleLabel(user, label);
    }

    public void importByUrl(UrlImportRequestDTO req, HttpServletRequest httpRequest) {

        final String pythonBin = "python";
        final String importScript = Path.of(
                System.getProperty("user.dir"), "scripts", "import_schedule.py"
        ).toString();

        User user = currentUser(httpRequest);
        List<ImportedEventDTO> imported = runPython(req, pythonBin, importScript);
        System.out.println("DTOs citite = " + imported.size());

        String label = buildLabel(req);
        List<Event> events = imported.stream()
                .flatMap(dto -> expand(dto, req, user, label).stream())
                .toList();

        System.out.println("Events de salvat = " + events.size());

        eventRepository.saveAll(events);
    }

    private User currentUser(HttpServletRequest http) {
        HttpSession s = http.getSession(false);
        if (s == null || s.getAttribute("user") == null)
            throw new IllegalStateException("No user logged in");

        String username = s.getAttribute("user").toString();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }

    private List<ImportedEventDTO> runPython(UrlImportRequestDTO req,
                                             String pythonBin,
                                             String importScript) {

        List<String> cmd = new ArrayList<>(List.of(
                pythonBin, importScript,
                req.getUrl(), req.getSheetName(), req.getGroupName(),
                "--startDate", req.getStartDate().toString(),
                "--endDate",   req.getEndDate().toString()
        ));
        if (StringUtils.hasText(req.getSeries())) {
            cmd.add("--series"); cmd.add(req.getSeries());
        }

        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            if (!p.waitFor(60, TimeUnit.SECONDS))
                throw new IllegalStateException("Python script timed out");

            List<ImportedEventDTO> list = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                int lineNo = 0;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    System.out.println("RAW-" + lineNo + ": " + line);
                    int idx = line.indexOf('{');
                    if (idx < 0) continue;                 // skip non-JSON noise
                    String json = line.substring(idx).trim();
                    try {
                        ImportedEventDTO dto = objectMapper.readValue(json, ImportedEventDTO.class);
                        if (StringUtils.hasText(dto.getStartTime()) && StringUtils.hasText(dto.getEndTime()))
                            list.add(dto);
                    } catch (IOException ex) {
                        System.err.println("⚠  Nu pot parsa linia " + lineNo + ": " + ex.getMessage());
                        System.err.println("    >> " + json);
                    }
                }
            }
            return list;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Error executing python import", e);
        }
    }

    private List<Event> expand(ImportedEventDTO dto, UrlImportRequestDTO req, User user, String label) {
        DayOfWeek iso = roToIso(dto.getDayOfWeek());
        System.out.println("DBG-day «" + dto.getDayOfWeek() + "» → " + iso);
        if (iso == null) return List.of();

        LocalTime st = LocalTime.parse(dto.getStartTime());
        LocalTime et = LocalTime.parse(dto.getEndTime());

        List<Event> list = new ArrayList<>();
        for (LocalDate d = req.getStartDate(); !d.isAfter(req.getEndDate()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == iso) {
                Event ev = new Event();
                ev.setTitle(dto.getTitle());
                ev.setDescription(dto.getDescription());
                ev.setStartTime(LocalDateTime.of(d, st));
                ev.setEndTime(LocalDateTime.of(d, et));
                ev.setUser(user);
                ev.setImported(true);
                ev.setScheduleLabel(label);
                list.add(ev);
            }
        }
        return list;
    }

    private static DayOfWeek roToIso(String zi) {
        if (zi == null) return null;

        zi = zi.strip();                                       // elimină spaţii
        zi = zi.replace('Ș','S').replace('Ş','S')
                .replace('ș','s').replace('ş','s')
                .replace('Ț','T').replace('Ţ','T')
                .replace('ț','t').replace('ţ','t')
                .replace('Ă','A').replace('Â','A')
                .replace('ă','a').replace('â','a')
                .replace('Î','I').replace('î','i');             // transliterare directă

        zi = java.text.Normalizer.normalize(zi,
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")                      // restul diacriticelor
                .replaceAll("[^A-Za-z]", "")                   // păstrează doar litere
                .toUpperCase();                                // LUNI, MARTI …

        return switch (zi) {
            case "LUNI"     -> DayOfWeek.MONDAY;
            case "MARTI"    -> DayOfWeek.TUESDAY;
            case "MIERCURI" -> DayOfWeek.WEDNESDAY;
            case "JOI"      -> DayOfWeek.THURSDAY;
            case "VINERI"   -> DayOfWeek.FRIDAY;
            case "SAMBATA"  -> DayOfWeek.SATURDAY;
            case "DUMINICA" -> DayOfWeek.SUNDAY;
            default -> {
                System.err.println("‼ Zi necunoscută: «" + zi + "»");
                yield null;
            }
        };
    }





    private String buildLabel(UrlImportRequestDTO r) {
        return String.join("-", r.getSheetName(), r.getGroupName(),
                        StringUtils.hasText(r.getSeries()) ? r.getSeries() : "")
                .replaceAll("-+$", "");
    }

    private void saveEventFromDto(ImportedEventDTO eventDto, String username, String scheduleLabel) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));
        Event ev = new Event();
        ev.setTitle(eventDto.getTitle());
        ev.setDescription(eventDto.getDescription());
        ev.setStartTime(LocalDateTime.parse(eventDto.getStartTime()));
        ev.setEndTime(LocalDateTime.parse(eventDto.getEndTime()));
        ev.setUser(user);
        ev.setImported(true);
        ev.setScheduleLabel(scheduleLabel);

        eventRepository.save(ev);
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
