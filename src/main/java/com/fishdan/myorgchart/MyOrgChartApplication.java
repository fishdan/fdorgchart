package com.fishdan.myorgchart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyOrgChartApplication {

    public static void main(String[] args) {

        System.out.println("Database Password: " + System.getProperty("spring.datasource.password"));
        SpringApplication.run(MyOrgChartApplication.class, args);
    }

}
