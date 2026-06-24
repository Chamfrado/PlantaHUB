package com.plantahub.api;

import com.plantahub.api.integration.infinitepay.InfinitePayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(InfinitePayProperties.class)
@SpringBootApplication
public class PlantahubApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlantahubApiApplication.class, args);
	}

}
