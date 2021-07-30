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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CrawlerAragog {
    private final CrawlerAragogDao dao = new JdbcCrawlerAragogDao();

    public void run () throws SQLException, IOException {
         String link;

        // 先从数据库中拿出来一个链接（拿出来并删除之），准备处理之
        while ((link = dao.getNextLinkThenDelete()) != null) {
            // 询问数据库是不是处理过了
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                System.out.println("link = " + link);
                Document doc = httpGetAndParseHtml(link);
                parseUrlsFromPageAndStoreIntoDatabase(doc);
                storeIntoDatabaseIfItIsNewsPage(doc, link);
                dao.updateDatabase(link, "INSERT INTO LINK_ALREADY_PROCESSED \n" +
                        "(LINK)\n" +
                        "VALUES (?)");
            }
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        new CrawlerAragog().run();
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                dao.updateDatabase(href, "INSERT INTO LINK_TO_BE_PROCESSED \n" +
                        "(LINK)\n" +
                        "VALUES (?)");
            }
        }
    }

    // 假如这是一个新闻的详情页面，就存入数据库，否则就什么都不做
    private void storeIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).html();
                String content =  articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                System.out.println("title = " + title);
                dao.insertNewsIntoDatabase(link, title, content);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        // 这是我们感兴趣的，我们只处理新浪内部的链接
        CloseableHttpClient httpclient = HttpClients.createDefault();
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
