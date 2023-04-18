package com.imtilab.bittracer.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TestService {
    public String test() {
        return "service alive";
    }

    public Object getData() {
        return null;
    }

//    @Async
    public void execute() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        ExecThread execThread = new ExecThread();
        System.out.println("Thread started");
        executorService.execute(execThread);
        executorService.shutdown();
        while (!executorService.isTerminated()) {
        }
        System.out.println("Thread terminated");
    }

}
