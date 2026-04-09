package com.fishdan.myorgchart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.time.Clock;

@SpringBootApplication
public class MyOrgChartApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyOrgChartApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SesV2Client sesV2Client() {
        return SesV2Client.builder()
            .region(Region.US_EAST_1)
            .build();
    }
}
