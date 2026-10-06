package com.amith.executor.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FileAnalyzer {
    private final ExecutorService executor;

    public FileAnalyzer(int numberOfTheads) {
        this.executor = Executors.newFixedThreadPool(numberOfTheads);
    }

    public void run() throws Exception {
        List<String> files = List.of(
                "file-01.txt",
                "file-02.txt",
                "file-03.txt",
                "file-04.txt",
                "file-05.txt",
                "file-06.txt",
                "file-07.txt",
                "file-08.txt",
                "file-09.txt",
                "file-10.txt");
        
        List<Future<FileResult>> futures = new ArrayList<>();
        
        //submit tasks
        for(String file: files) {
            FileTask task = new FileTask(file);
            Future<FileResult> future = executor.submit(task);
            futures.add(future);
        }
        System.out.println("\nAll tasks submitted. \n");

        // Collect Results
        for(Future<FileResult> future: futures) {
            FileResult result = future.get();
            System.out.println(result);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n Executor terminated.");
    }
}
