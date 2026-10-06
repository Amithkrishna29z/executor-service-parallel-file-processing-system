package com.amith.executor.pool;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FixedThreadPoolExample {
    public static void run() {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for(int i=1;i<=10;i++) {
            int taskId =i;
            executor.submit(()-> {
                try {
                    System.out.println("START Task "+taskId+" -> "+Thread.currentThread().getName());

                    Thread.sleep(2000);

                    System.out.println("End Task "+taskId+ " -> "+Thread.currentThread().getName());
                }catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        executor.shutdown();
    }
}
