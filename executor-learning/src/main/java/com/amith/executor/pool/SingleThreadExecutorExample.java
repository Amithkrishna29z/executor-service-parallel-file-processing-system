package com.amith.executor.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadExecutorExample {
    public static void run() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        for(int i =1;i<=5;i++) {
            int taskId =i;

            executor.submit(()-> {
                System.out.println("Task "+taskId +" -> "+Thread.currentThread().getName());
            });
        }
        executor.shutdown();
    }
}
