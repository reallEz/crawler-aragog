package com.github.reallEz;

import java.sql.SQLException;

public interface CrawlerAragogDao {
    String getNextLink(String sql) throws SQLException;
    String getNextLinkThenDelete() throws SQLException;
    void updateDatabase(String link, String sql) throws SQLException;
    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;
    boolean isLinkProcessed(String link) throws SQLException;
}
