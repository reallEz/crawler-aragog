package com.github.reallEz;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.Instant;

public class News {
    private Integer id;
    private String url;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant modifyAt;

    public News(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Instant getModifyAt() {
        return modifyAt;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setModifyAt(Instant modifyAt) {
        this.modifyAt = modifyAt;
    }
}
