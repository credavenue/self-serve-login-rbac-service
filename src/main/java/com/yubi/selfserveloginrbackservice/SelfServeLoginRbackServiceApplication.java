package com.yubi.selfserveloginrbackservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class SelfServeLoginRbackServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SelfServeLoginRbackServiceApplication.class, args);
	}

}
