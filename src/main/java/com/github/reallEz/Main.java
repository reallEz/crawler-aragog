package com.github.reallEz;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    private static List<String> loadUrlFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(2));
            }
        }
        return results;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:file:/Users/zhulian/Documents/hcsp/crawler-aragog/news",
                "root",
                "root");

        while (true) {
            // 待处理的链接池
            // 从数据库加载即将处理链接的代码
            List<String> linkPool = loadUrlFromDatabase(connection, "SELECT * FROM LINK_TO_BE_PROCESSED");
            // 已处理的链接池
            // 从数据库加载已处理的链接的代码
            Set<String> processedLink = new HashSet<>(
                    loadUrlFromDatabase(connection, "SELECT * FROM LINK_ALREADY_PROCESSED")
            );
            if (linkPool.isEmpty()) {
                break;
            }
            // 从待处理池子中捞出一个链接进行处理
            // 处理之后从池子（包括数据库）中删掉
            // ArrayList 从尾部删除更有效率
            String link = linkPool.remove(linkPool.size() - 1);
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM LINK_TO_BE_PROCESSED WHERE LINK = ?")) {
                statement.setString(1, link);
                statement.executeUpdate();
            }

            boolean flag = false;
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT * FROM LINK_ALREADY_PROCESSED WHERE LINK = ?")) {
                statement.setString(1, link);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    flag = true;
                }
            }
            if (flag) {
                continue;
            }


            // 我们只关心 news.sina 的，并且要排除登录页面
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                for (Element aTag : doc.select("a")) {
                    String href = aTag.attr("href");
                    linkPool.add(href);
                    try (PreparedStatement statement =
                                 connection.prepareStatement("INSERT INTO LINK_TO_BE_PROCESSED \n" +
                                         "(LINK)\n" +
                                         "VALUES (?)")) {
                        statement.setString(1, href);
                        statement.executeUpdate();
                    }
                }

                // 假如这是一个新闻的详情页面，就存入数据库，否则就什么都不做
                storeIntoDatabaseIfItIsNewsPage(doc);
                try (PreparedStatement statement =
                             connection.prepareStatement("INSERT INTO LINK_ALREADY_PROCESSED \n" +
                                     "(LINK)\n" +
                                     "VALUES (?)")) {
                    statement.setString(1, link);
                    statement.executeUpdate();
                }
            } else {
                // 这是我们不感兴趣的，不处理它
            }
        }
    }


    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).html();
                System.out.println("title = " + title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 这是我们感兴趣的，我们只处理新浪内部的链接
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        System.out.println("link = " + link);
        HttpGet httpGet = new HttpGet(link);
        httpGet.setHeader("use-Agent", "mozilla/5.0 (macintosh; intel mac os x 10_15_7) applewebkit/537.36 (khtml, like gecko) chrome/91.0.4472.164 safari/537.36");

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

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
