package com.example.studybuddy.service;

import com.example.studybuddy.repository.dto.EventRequestDTO;
import com.example.studybuddy.repository.dto.EventResponseDTO;
import com.example.studybuddy.repository.entity.Event;

import java.util.List;

public interface EventService {
    public List<EventResponseDTO> getUserEvents(String username);
    public EventResponseDTO createEvent(EventRequestDTO requestDTO);
    public EventResponseDTO updateEvent(Long id, EventRequestDTO requestDTO);
    public void deleteEvent(Long id);

    }
