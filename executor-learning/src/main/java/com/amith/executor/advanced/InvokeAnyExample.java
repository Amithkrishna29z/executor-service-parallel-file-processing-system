package com.amith.executor.advanced;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InvokeAnyExample {
    public static void run() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Callable<String>> tasks = List.of(

            ()-> {
                Thread.sleep(3000);
                return "Server A";
            },

            ()-> {
                Thread.sleep(1000);
                return "Server B";
            },
            ()-> {
                Thread.sleep(2000);
                return "Server C";
            }
        );

        String result = executor.invokeAny(tasks);

        System.out.println("First result = "+result);

        executor.shutdown();
    }
}
