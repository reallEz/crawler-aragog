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
import org.jsoup.select.Elements;

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

            if (link.contains("news.sina.cn") || "https://sina.cn/".equals(link)) {
                // 这是我们感兴趣的，我们只处理新浪内部的链接
                if (link.startsWith("//")) {
                    link = "https:" + link;
                }
                System.out.println("link = " + link);
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(link);
                httpGet.setHeader("use-Agent", "mozilla/5.0 (macintosh; intel mac os x 10_15_7) applewebkit/537.36 (khtml, like gecko) chrome/91.0.4472.164 safari/537.36");

                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    System.out.println(response.getStatusLine());
                    HttpEntity entity = response.getEntity();
                    String html = EntityUtils.toString(entity);
                    Document doc = Jsoup.parse(html);
                    ArrayList<Element> links = doc.select("a");

                    for (Element aTag : links) {
                        linkPool.add(aTag.attr("href"));
                    }


                    // 假如这是一个新闻的详情页面，就存入数据库，否则就什么都不做
                    ArrayList<Element> articleTags = doc.select("article");
                    if (!articleTags.isEmpty()) {
                        for (Element articleTag : articleTags) {
                            String title = articleTags.get(0).child(0).html();
                            System.out.println("title = " + title);
                        }
                    }
                    processedLink.add(link);
                }
            } else {
                // 这是我们不感兴趣的，不处理它
            }
        }
    }
}
