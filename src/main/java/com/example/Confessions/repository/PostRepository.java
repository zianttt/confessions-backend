package com.example.Confessions.repository;

import com.example.Confessions.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository provides functionalities like findAll(), delete()...
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
