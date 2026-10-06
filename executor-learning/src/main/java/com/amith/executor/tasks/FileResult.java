package com.amith.executor.tasks;

public class FileResult {

    private final String fileName;
    private final int lines;
    private final int words;
    private final String threadName;

    public FileResult(String fileName, int lines, int words, String threadName) {
        this.fileName= fileName;
        this.lines = lines;
        this.words = words;
        this.threadName = threadName;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLines() {
        return lines;
    }

    public int getWords() {
        return words;
    }

    public String getThreadName() {
        return threadName;
    }

    @Override 
    public String toString() {
        return String.format("%-15s lines=%-5d Words=%-5d Thread=%s", fileName,lines,words,threadName);
    }
}
