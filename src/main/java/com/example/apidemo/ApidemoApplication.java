package com.example.apidemo;

import com.example.apidemo.config.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties.class)
public class ApidemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApidemoApplication.class, args);
	}

}
