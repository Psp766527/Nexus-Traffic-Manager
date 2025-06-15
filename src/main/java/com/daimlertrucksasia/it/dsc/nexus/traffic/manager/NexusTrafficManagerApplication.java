package com.daimlertrucksasia.it.dsc.nexus.traffic.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NexusTrafficManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusTrafficManagerApplication.class, args);
    }

}
