package de.bhtberlin.se2.hellocontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		System.out.println("Hello Spring Boot");
		SpringApplication.run(Application.class, args);
	}

}
