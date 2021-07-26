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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        // 待处理的链接池
        List<String> linkPool = new ArrayList<>();
        // 已处理的链接池
        Set<String> processedLink = new HashSet<>();
        linkPool.add("https://sina.cn/");

        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }

            // ArrayList 从尾部删除更有效率
            String link = linkPool.remove(linkPool.size() - 1);

            if (processedLink.contains(link)) {
                continue;
            }

            // 我们只关心 news.sina 的，并且要排除登录页面
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                doc.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);

                // 假如这是一个新闻的详情页面，就存入数据库，否则就什么都不做
                storeIntoDatabaseIfItIsNewsPage(doc);
                processedLink.add(link);

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
