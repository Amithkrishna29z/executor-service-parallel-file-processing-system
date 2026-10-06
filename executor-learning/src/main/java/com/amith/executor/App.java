package com.amith.executor;

import com.amith.executor.tasks.FileAnalyzer;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("=========================");
        System.out.println("ExecutorService Learning Lab");
        System.out.println("=========================");

        FileAnalyzer analyzer = new FileAnalyzer(4);

        analyzer.run();

        System.out.println("\n Application finished");
    }
}
