package com.github.ruediste.salta.jsr330.wikiExamples;

public class DatabaseTransactionLog implements TransactionLog {

    @Override
    public void log(String message) {
        System.out.println("Database Transaction Log: " + message);
    }

}
