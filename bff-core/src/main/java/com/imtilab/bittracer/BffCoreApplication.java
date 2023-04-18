package com.imtilab.bittracer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BffCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffCoreApplication.class, args);
    }

//    @Bean("asyncExecutor")
//    public TaskExecutor getAsyncExecutor(){
//        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
//        threadPoolTaskExecutor.setCorePoolSize(20);
//        threadPoolTaskExecutor.setMaxPoolSize(1000);
//        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
//        threadPoolTaskExecutor.setThreadNamePrefix("Aysnc-");
//        return threadPoolTaskExecutor;
//    }
}
