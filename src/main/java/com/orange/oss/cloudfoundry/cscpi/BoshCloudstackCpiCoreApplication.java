package com.orange.oss.cloudfoundry.cscpi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;

@SpringBootApplication
@ComponentScan(excludeFilters = @Filter(BeanMock.class))
      
public class BoshCloudstackCpiCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoshCloudstackCpiCoreApplication.class, args);
    }
}
