package com.example.Confessions.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
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
    @JsonFormat(pattern="yyyy-MM-dd' 'HH:mm:ss")
    private Date datePosted;

    @Column(name = "approve")
    private int approve;

    public Post() {

    }

    public Post(int approve, String content, Date datePosted, long replyId) {
        this.content = content;
        this.replyId = replyId;
        this.datePosted = datePosted;
        this.approve = approve;
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

    public int getApprove() {
        return approve;
    }

    public void setApprove(int approve) {
        this.approve = approve;
    }
}
