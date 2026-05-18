package com.jobtracker.application;

import com.jobtracker.JobtrackerApplication;
import org.springframework.boot.SpringApplication;

public class TestJobtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(JobtrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
