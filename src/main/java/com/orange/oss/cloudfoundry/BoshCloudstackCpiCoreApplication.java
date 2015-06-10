package com.orange.oss.cloudfoundry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.annotation.IntegrationComponentScan;

@SpringBootApplication
@IntegrationComponentScan       
public class BoshCloudstackCpiCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoshCloudstackCpiCoreApplication.class, args);
    }
}
