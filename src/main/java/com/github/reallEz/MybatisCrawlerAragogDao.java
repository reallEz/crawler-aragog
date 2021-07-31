package com.github.reallEz;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MybatisCrawlerAragogDao implements CrawlerAragogDao {
    private SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerAragogDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String getNextLinkThenDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("com.github.reallEz.myMapper.selectNextAvailableLink");
            if (link != null) {
                session.delete("com.github.reallEz.myMapper.deleteLink", link);
            }
            return link;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.reallEz.myMapper.insertNews", new News(link, title, content));
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            int count = session.selectOne("com.github.reallEz.myMapper.countLink", link);
            return count != 0;
        }
    }

    @Override
    public void insertProcessedLink(String link) {
        Map<String, Object> params = new HashMap<>();
        params.put("tableName", "LINK_ALREADY_PROCESSED");
        params.put("LINK", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.reallEz.myMapper.insertLink", params);
        }
    }

    @Override
    public void insertLinkToBeProcessed(String link) {
        Map<String, Object> params = new HashMap<>();
        params.put("tableName", "LINK_TO_BE_PROCESSED");
        params.put("LINK", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.reallEz.myMapper.insertLink", params);
        }
    }
}
