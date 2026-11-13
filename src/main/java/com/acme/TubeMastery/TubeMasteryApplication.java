package com.acme.TubeMastery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TubeMasteryApplication {

	public static void main(String[] args) {
		SpringApplication.run(TubeMasteryApplication.class, args);
	}

}
