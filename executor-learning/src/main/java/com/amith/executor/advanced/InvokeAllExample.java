package com.amith.executor.advanced;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InvokeAllExample {

    public static void run() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<Integer>> tasks = new ArrayList<>();

        for(int i=1;i<=5;i++) {
            int number =i;

            tasks.add(()-> {
                Thread.sleep(1000);
                return number * number;
            });
        }

        List<Future<Integer>> futures = executor.invokeAll(tasks);

        for(Future<Integer> future : futures) {
            System.out.println("Result = "+future.get());
        }
        executor.shutdown();
    }
}
