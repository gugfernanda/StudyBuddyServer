package com.example.studybuddy.controller;

import com.example.studybuddy.repository.dto.EventRequestDTO;
import com.example.studybuddy.repository.dto.EventResponseDTO;
import com.example.studybuddy.service.EventService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@CrossOrigin(origins = "http://localhost:5173")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<List<EventResponseDTO>> getUserEvents(@PathVariable String username) {
        List<EventResponseDTO> events = eventService.getUserEvents(username);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(@RequestBody EventRequestDTO eventRequestDTO) {
        EventResponseDTO eventResponseDTO = eventService.createEvent(eventRequestDTO);
        return ResponseEntity.ok(eventResponseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDTO> updateEvent(@PathVariable Long id, @RequestBody EventRequestDTO requestDTO) {
        EventResponseDTO updatedEvent = eventService.updateEvent(id, requestDTO);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok("Event deleted successfully.");
    }
}
