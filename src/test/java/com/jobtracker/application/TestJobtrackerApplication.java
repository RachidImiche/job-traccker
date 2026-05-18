package com.jobtracker.application;

import com.jobtracker.JobTrackerApplication;
import org.springframework.boot.SpringApplication;

public class TestJobtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(JobTrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
