package kh.gov.smecu.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmeCuPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmeCuPortalApplication.class, args);
    }
}
