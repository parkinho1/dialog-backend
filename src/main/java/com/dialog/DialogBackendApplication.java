package com.dialog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.dialog.googleauth.domain.GoogleAuthDTO;

@SpringBootApplication
@EnableConfigurationProperties(GoogleAuthDTO.class)
@ComponentScan(basePackages = {"com.dialog", "com.dialog.exception"})
@EntityScan(basePackages = "com.dialog")
@EnableJpaRepositories(basePackages = "com.dialog")
public class DialogBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DialogBackendApplication.class, args);
	}

}