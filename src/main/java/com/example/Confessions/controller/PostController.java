package com.example.Confessions.controller;

import com.example.Confessions.model.Post;
import com.example.Confessions.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/")
public class PostController {

    // Inject repository here
    @Autowired
    private PostRepository postRepository;

    // get all posts at /posts api
    @GetMapping("/posts")
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }


    // create new posts
    @PostMapping("/posts")
    public Post createPost(@RequestBody Post post) {
        return postRepository.save(post);
    }

}
