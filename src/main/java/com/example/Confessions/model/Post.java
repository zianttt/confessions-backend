package com.example.Confessions.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "content")
    private String content;

    @Column(name = "reply_id")
    private long replyId;

    @Column(name = "date_posted")
    @JsonFormat(pattern="YYYY-MM-DD' 'HH:mm:ss")
    private Date datePosted;

    @Column(name = "submit_id")
    private long submitId;

    @Column(name = "has_file")
    private long hasFile;

    public Post() {

    }

    public Post(long submitId, String content, Date datePosted, long replyId, long hasFile) {
        this.content = content;
        this.replyId = replyId;
        this.datePosted = datePosted;
        this.submitId = submitId;
        this.hasFile = hasFile;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getReplyId() {
        return replyId;
    }

    public void setReplyId(long replyId) {
        this.replyId = replyId;
    }

    public Date getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(Date datePosted) {
        this.datePosted = datePosted;
    }

    public long getSubmitId() {
        return submitId;
    }

    public void setSubmitId(long submitId) {
        this.submitId = submitId;
    }

    public long getHasFile() {
        return hasFile;
    }

    public void setHasFile(long hasFile) {
        this.hasFile = hasFile;
    }
}
