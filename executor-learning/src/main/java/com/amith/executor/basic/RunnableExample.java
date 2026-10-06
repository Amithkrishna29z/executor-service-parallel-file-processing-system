package com.amith.executor.basic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunnableExample {
    public static void run() {

        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 10; i++) {

            int taskId = i;

            executor.submit(() -> {

                System.out.println(
                        "Task " + taskId +
                                " running on " +
                                Thread.currentThread().getName());

            });
        }
        executor.shutdown();
    }
}
