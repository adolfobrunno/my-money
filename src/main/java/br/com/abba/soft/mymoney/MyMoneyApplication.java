package br.com.abba.soft.mymoney;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyMoneyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyMoneyApplication.class, args);
    }

}
