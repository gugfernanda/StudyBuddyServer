package com.example.studybuddy.controller;

import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.ManualScheduleDTO;
import com.example.studybuddy.repository.dto.ScheduleImportRequestDTO;
import com.example.studybuddy.repository.dto.UrlImportRequestDTO;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.implementation.ScheduleImportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedule")
public class ScheduleImportController {
    private final ScheduleImportService importService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public ScheduleImportController(ScheduleImportService importService, UserRepository userRepository, EventRepository eventRepository) {
        this.importService = importService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }


    @DeleteMapping("/clear")
    public ResponseEntity<?> clearImportedSchedule(@RequestParam String label, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No user is logged in");
        }

        String username = session.getAttribute("user").toString();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        eventRepository.deleteAllByUserAndScheduleLabelAndImportedIsTrue(user, label);

        return ResponseEntity.ok("Schedule with label '" + label + "' has been cleared.");
    }

    @PostMapping("/manual")
    public ResponseEntity<?> addManualSchedule(@RequestBody ManualScheduleDTO dto, HttpServletRequest httpRequest) {
        importService.importManualSchedule(dto, httpRequest);
        return ResponseEntity.ok("Manual schedule imported successfully!");
    }

    @DeleteMapping("/label/{label}")
    public ResponseEntity<?> deleteManualSchedule(@PathVariable String label, HttpServletRequest httpRequest) {
        importService.deleteScheduleByLabel(label, httpRequest);
        return ResponseEntity.ok("All events with label '" + label + "' deleted successfully!");
    }

    @PostMapping("/importByUrl")
    public ResponseEntity<?> importByUrl(@RequestBody UrlImportRequestDTO request, HttpServletRequest httpRequest) {
        importService.importByUrl(request, httpRequest);
        return ResponseEntity.ok("Url imported successfully!");
    }

}
