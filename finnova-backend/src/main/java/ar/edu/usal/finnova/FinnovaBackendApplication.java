package ar.edu.usal.finnova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinnovaBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(FinnovaBackendApplication.class, args);
	}
}