package com.example.myapp.web.rest.vm;

public class Greeting {
    private long count;
    private String content;

    public Greeting(long count, String content) {
        this.count = count;
        this.content = content;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
