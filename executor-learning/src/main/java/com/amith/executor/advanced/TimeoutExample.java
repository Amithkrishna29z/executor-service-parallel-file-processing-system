package com.amith.executor.advanced;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutExample {

    public static void run() {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<String> future = executor.submit(()-> {
            Thread.sleep(5000);
            return "Completed";
        });

        try {
            String result = future.get(2, TimeUnit.SECONDS);
            System.out.println(result);
        }catch(TimeoutException e ) {
            System.out.println("Task took too long");
            future.cancel(true);
        }catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }catch(ExecutionException e) {
             System.out.println(
                    "Task failed: " +
                    e.getCause()
            );
        }
        executor.shutdown();
    }
}
