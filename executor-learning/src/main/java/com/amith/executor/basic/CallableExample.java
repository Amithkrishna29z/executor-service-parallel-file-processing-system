package com.amith.executor.basic;

import java.util.concurrent.Callable;

public class CallableExample {
    public static Callable<Integer> createTask(int number) {
        return ()-> {
            System.out.println("Calculating "+number+" on "+Thread.currentThread().getName());
            return number*number;
        };
    }
}
