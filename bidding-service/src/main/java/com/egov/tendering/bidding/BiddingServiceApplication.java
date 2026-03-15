package com.egov.tendering.bidding;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.egov.tendering.bidding.client")
public class BiddingServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(BiddingServiceApplication.class, args);
  }
}
