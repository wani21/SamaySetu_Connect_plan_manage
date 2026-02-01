package com.College.timetable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class samaysetuApplication {

	public static void main(String[] args) {
		SpringApplication.run(samaysetuApplication.class, args);
	}

}
