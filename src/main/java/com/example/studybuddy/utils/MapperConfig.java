package com.example.studybuddy.utils;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true) // Permite maparea automată a câmpurilor cu același nume
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE); // Accesează și câmpurile private
        return modelMapper;
    }
}
