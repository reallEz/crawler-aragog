package com.github.reallEz;

import java.sql.SQLException;

public interface CrawlerAragogDao {
    String getNextLinkThenDelete() throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertProcessedLink(String link);

    void insertLinkToBeProcessed(String link);
}
