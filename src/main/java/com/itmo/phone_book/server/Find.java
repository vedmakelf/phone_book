package com.itmo.phone_book.server;

public class Find implements Command {
    private final String keyword;

    public Find(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
