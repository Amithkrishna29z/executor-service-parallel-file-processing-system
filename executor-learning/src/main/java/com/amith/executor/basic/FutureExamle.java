package com.amith.executor.basic;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FutureExamle {
    public static void run() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        Callable<Integer> task =()-> {
            Thread.sleep(2000);
            return 100;
        };

        Future<Integer> future = executor.submit(task);

        System.out.println("Task submitted");
        System.out.println("Doing other work...");

        Integer result = future.get();
        System.out.println("Result = "+result);
        executor.shutdown();
    }
}
