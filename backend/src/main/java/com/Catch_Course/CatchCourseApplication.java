package com.Catch_Course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableJpaAuditing
public class CatchCourseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatchCourseApplication.class, args);
	}

}
