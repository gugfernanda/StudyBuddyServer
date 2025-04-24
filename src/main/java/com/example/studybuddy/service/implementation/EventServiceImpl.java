package com.example.studybuddy.service.implementation;

import com.example.studybuddy.repository.UserRepository;
import com.example.studybuddy.repository.dto.EventRequestDTO;
import com.example.studybuddy.repository.dto.EventResponseDTO;
import com.example.studybuddy.repository.entity.Event;
import com.example.studybuddy.repository.EventRepository;
import com.example.studybuddy.repository.entity.User;
import com.example.studybuddy.service.EventService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final UserRepository userRepository;
    private EventRepository eventRepository;

    public EventServiceImpl(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<EventResponseDTO> getUserEvents(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Event> events = eventRepository.findByUser(user);
        return events.stream()
                .map(event -> new EventResponseDTO(
                        event.getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getStartTime(),
                        event.getEndTime(),
                        event.getUser().getUsername()
                )).collect(Collectors.toList());
    }

    @Override
    public EventResponseDTO createEvent(EventRequestDTO requestDTO) {
        User user = userRepository.findByUsername(requestDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = new Event();
        event.setTitle(requestDTO.getTitle());
        event.setDescription(requestDTO.getDescription());
        event.setStartTime(requestDTO.getStartTime());
        event.setEndTime(requestDTO.getEndTime());
        event.setScheduleLabel(null);
        event.setImported(false);
        event.setUser(user);

        Event savedEvent = eventRepository.save(event);

        return new EventResponseDTO(
                savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getDescription(),
                savedEvent.getStartTime(),
                savedEvent.getEndTime(),
                savedEvent.getUser().getUsername());
    }

    @Override
    public EventResponseDTO updateEvent(Long id, EventRequestDTO requestDTO) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setTitle(requestDTO.getTitle());
        event.setDescription(requestDTO.getDescription());
        event.setStartTime(requestDTO.getStartTime());
        event.setEndTime(requestDTO.getEndTime());

        Event updatedEvent = eventRepository.save(event);

        return new EventResponseDTO(
                updatedEvent.getId(),
                updatedEvent.getTitle(),
                updatedEvent.getDescription(),
                updatedEvent.getStartTime(),
                updatedEvent.getEndTime(),
                updatedEvent.getUser().getUsername());
    }

    @Override
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found");
        }
        eventRepository.deleteById(id);
    }


}
