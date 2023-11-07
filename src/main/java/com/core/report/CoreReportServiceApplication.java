package com.core.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreReportServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreReportServiceApplication.class, args);
	}

}
