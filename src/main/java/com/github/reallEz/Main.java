package com.github.reallEz;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Main {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";


    private static String getNextLink(Connection connection, String sql) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql); ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(2);
            }
        }
        return null;
    }

    private static String getNextLinkThenDelete (Connection connection) throws SQLException {
        String link = getNextLink(connection, "SELECT * FROM LINK_TO_BE_PROCESSED LIMIT 1");
        if (link != null) {
            updateDatabase(connection, link, "DELETE FROM LINK_TO_BE_PROCESSED WHERE LINK = ?");
        }
        // 处理之后从池子（包括数据库）中删掉
        return link;
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection =
                DriverManager.getConnection(
                        "jdbc:h2:file:/Users/zhulian/Documents/hcsp/crawler-aragog/news", USER_NAME, PASSWORD);

        String link;

        // 先从数据库中拿出来一个链接（拿出来并删除之），准备处理之
        while ((link = getNextLinkThenDelete(connection)) != null) {

            // 询问数据库是不是处理过了
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                System.out.println("link = " + link);
                Document doc = httpGetAndParseHtml(link);

                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);
                storeIntoDatabaseIfItIsNewsPage(connection, doc, link);
                updateDatabase(connection, link, "INSERT INTO LINK_ALREADY_PROCESSED \n" +
                        "(LINK)\n" +
                        "VALUES (?)");
            }

        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                updateDatabase(connection, href, "INSERT INTO LINK_TO_BE_PROCESSED \n" +
                        "(LINK)\n" +
                        "VALUES (?)");
            }
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    private static void updateDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    // 假如这是一个新闻的详情页面，就存入数据库，否则就什么都不做
    private static void storeIntoDatabaseIfItIsNewsPage(Connection connection, Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).html();
                String content =  articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                System.out.println("title = " + title);
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
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 这是我们感兴趣的，我们只处理新浪内部的链接

        System.out.println("link = " + link);
        HttpGet httpGet = new HttpGet(link);
        httpGet.setHeader("use-Agent", "mozilla/5.0 (macintosh; intel mac os x 10_15_7) applewebkit/537.36 (khtml, like gecko) chrome/91.0.4472.164 safari/537.36");

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

    // 我们只关心 news.sina 的，并且要排除登录页面
    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn/".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}
