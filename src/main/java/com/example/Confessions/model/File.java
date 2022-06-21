package com.example.Confessions.model;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "files")
public class File {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String name;

    private String type;

    @Column(name = "submit_id")
    private long submitId;

    @Lob
    private byte[] data;

    public File() {
    }

    public File(String name, String type, byte[] data, long submitId) {
        this.name = name;
        this.type = type;
        this.data = data;
        this.submitId = submitId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getSubmitId() {
        return submitId;
    }

    public void setSubmitId(long submitId) {
        this.submitId = submitId;
    }
}