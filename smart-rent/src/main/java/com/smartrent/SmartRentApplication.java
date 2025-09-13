package com.smartrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SmartRentApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartRentApplication.class, args);
	}

}
