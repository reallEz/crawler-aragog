package com.github.reallEz;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
public class JdbcCrawlerAragogDao implements CrawlerAragogDao {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";

    Connection connection;

    public JdbcCrawlerAragogDao(){
        try {
            connection = DriverManager.getConnection(
                    "jdbc:h2:file:/Users/zhulian/Documents/hcsp/crawler-aragog/news", USER_NAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getNextLink(String sql) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql); ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(2);
            }
        }
        return null;
    }

    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("SELECT * FROM LINK_TO_BE_PROCESSED LIMIT 1");
        if (link != null) {
            updateDatabase(link, "DELETE FROM LINK_TO_BE_PROCESSED WHERE LINK = ?");
        }
        // 处理之后从池子（包括数据库）中删掉
        return link;
    }

    public void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO NEWS \n" +
                "(URL, TITLE, CONTENT, CREATED_AT, MODIFY_AT) \n" +
                "VALUES \n" +
                "(?, ?, ?, now(), now())"
        )) {
            statement.setString(1, link);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        }
    }

    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT * FROM LINK_ALREADY_PROCESSED WHERE LINK = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }
}
