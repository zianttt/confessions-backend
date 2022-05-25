package com.example.Confessions.model;

import javax.persistence.*;

@Entity
@Table(name = "displayId")
public class DisplayId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "display_id")
    private long displayId;

    @Column(name = "map_to_post")
    private long mapToPost;


    public DisplayId() {

    }

    public DisplayId(long displayId, long mapToPost) {
        this.displayId = displayId;
        this.mapToPost = mapToPost;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDisplayId() {
        return displayId;
    }

    public void setDisplayId(long displayId) {
        this.displayId = displayId;
    }

    public long getMapToPost() {
        return mapToPost;
    }

    public void setMapToPost(long mapToPost) {
        this.mapToPost = mapToPost;
    }
}
