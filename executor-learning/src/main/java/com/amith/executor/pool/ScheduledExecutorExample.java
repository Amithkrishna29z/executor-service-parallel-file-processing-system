package com.amith.executor.pool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorExample {
    public static void run() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

        executor.schedule(()-> System.out.println("Executed after 3 seconds"), 3, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(()->System.out.println("Periodic task: "+System.currentTimeMillis()), 1, 2,TimeUnit.SECONDS);

        executor.schedule(executor::shutdown, 10, TimeUnit.SECONDS);
    }
}
