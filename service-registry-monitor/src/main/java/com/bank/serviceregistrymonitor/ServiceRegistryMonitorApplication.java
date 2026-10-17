package com.bank.serviceregistrymonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryMonitorApplication.class, args);
    }

}
