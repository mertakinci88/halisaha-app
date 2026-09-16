package com.msc.halisaha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HalisahaBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HalisahaBeApplication.class, args);
    }

}
