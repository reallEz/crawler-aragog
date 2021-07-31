package com.github.reallEz;

public class Main {
    public static void main(String[] args) {
        CrawlerAragogDao dao = new MybatisCrawlerAragogDao();
        for (int i = 0; i < 9; i++) {
            new CrawlerAragog(dao).run();
        }
    }
}
