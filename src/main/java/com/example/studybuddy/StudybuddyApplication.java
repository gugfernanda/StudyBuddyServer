package com.example.studybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class StudybuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudybuddyApplication.class, args);
	}

}
