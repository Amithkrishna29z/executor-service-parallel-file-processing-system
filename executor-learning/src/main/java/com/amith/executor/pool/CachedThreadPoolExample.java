package com.amith.executor.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CachedThreadPoolExample {
    public static void run() {
        ExecutorService executor = Executors.newCachedThreadPool();

        for(int i=1;i<=20;i++) {
            int taskId =i;

            executor.submit(()-> {
                System.out.println("Task "+taskId+ " -> "+Thread.currentThread().getName());
            });
        }
        executor.shutdown();
    }
}
