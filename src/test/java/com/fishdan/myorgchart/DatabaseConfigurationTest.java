package com.fishdan.myorgchart;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigurationTest {

    @Test
    void baseConfigurationRequiresEnvironmentProvidedDatasourceUrl() throws IOException {
        Properties properties = loadProperties("application.properties");

        assertEquals("${SPRING_DATASOURCE_URL}", properties.getProperty("spring.datasource.url"));
    }

    @Test
    void devProfileDefaultsDatasourceUrlToLocalMariaDb() throws IOException {
        Properties properties = loadProperties("application-dev.properties");

        assertEquals(
            "${SPRING_DATASOURCE_URL:jdbc:mariadb://localhost:3306/myorgchart}",
            properties.getProperty("spring.datasource.url")
        );
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            properties.load(inputStream);
        }

        return properties;
    }
}
