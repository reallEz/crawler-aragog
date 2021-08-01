package com.github.reallEz;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    static long start = System.currentTimeMillis();
    private static void mockData(SqlSessionFactory sqlSessionFactory, int howMany) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> currentNews = session.selectList("com.github.reallEz.mockMapper.selectNews");

            try {
                int count = howMany - currentNews.size();
                Random r = new Random();
                while (count-- > 0) {
                    int index = r.nextInt(currentNews.size());
                    News newsToBeInserted = currentNews.get(index);
                    Instant currentTime = newsToBeInserted.getCreatedAt().minusSeconds(r.nextInt(60 * 60 * 24 * 15));
                    newsToBeInserted.setCreatedAt(currentTime);
                    newsToBeInserted.setModifyAt(currentTime);
                    session.insert("com.github.reallEz.mockMapper.insertNews", newsToBeInserted);
                    System.out.print("index = ");
                    System.out.print(index + ", ");
                    if (count % 50 == 0) {
                        System.out.println("");
                        System.out.print("index = ");
                    }
                    if (count % 200 == 0) {
                        System.out.println("Left: " + count);
                        System.out.println("");
                        System.out.println("current time usage = " + Long.toString(System.currentTimeMillis() - start));
                        session.flushStatements();
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mockData(sqlSessionFactory, 1_0000_0000);
    }
}
