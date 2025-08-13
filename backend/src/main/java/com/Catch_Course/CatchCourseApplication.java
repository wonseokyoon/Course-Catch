package com.Catch_Course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

@EnableScheduling
@SpringBootApplication
@EnableJpaAuditing
public class CatchCourseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatchCourseApplication.class, args);
	}

	@Bean
	public Supplier<ZonedDateTime> clockSupplier() {
		return () -> ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
	}
}
