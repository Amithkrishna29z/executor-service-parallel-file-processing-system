package com.amith.executor.tasks;

import java.util.concurrent.Callable;

public class FileTask implements Callable<FileResult> {
    private final String fileName;

    public FileTask(String fileName) {
        this.fileName = fileName;
    }

    @Override 
    public FileResult call() throws Exception {
        String threadName = Thread.currentThread().getName();

        System.out.println("START Processing "+ fileName+" on "+threadName);

        Thread.sleep(1000);

        int lines = (int) (Math.random()*500);
        int words = lines * 10;

        System.out.println("End processing "+ fileName+" on "+threadName);

        return new FileResult(fileName, lines, words, threadName);
    }
}
