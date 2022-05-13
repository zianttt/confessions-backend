package com.example.Confessions.controller;

import com.example.Confessions.exception.ResourceNotFoundException;
import com.example.Confessions.model.Post;
import com.example.Confessions.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

    // create new posts api
    @PostMapping("/posts")
    public Post createPost(@RequestBody Post post) {
        return postRepository.save(post);
    }

    // get post by id {} means path variable
    @GetMapping("/posts/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));
        return ResponseEntity.ok(post);

    }

    // approve post api
    @PutMapping("/posts/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @RequestBody Post updatedPost) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));

        post.setContent(updatedPost.getContent());
        post.setApprove(updatedPost.getApprove());
        post.setDatePosted(updatedPost.getDatePosted());
        post.setReplyId(updatedPost.getReplyId());

        Post savedPost = postRepository.save(post);
        return ResponseEntity.ok(savedPost);
    }

    // delete post api
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, Boolean>> deletePost(@PathVariable Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));
        postRepository.delete(post);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    // delete posts by batch
    @DeleteMapping("/posts/{id}/_batch")
    public ResponseEntity<Map<String, Boolean>> deletePostBatch(@PathVariable Long id) {
        //List<Long> postIdsList = Arrays.asList(postIds.split(" ")).stream().map(Long::parseLong).collect(Collectors.toList());
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));

        List<Post> allPosts = postRepository.findAll();
        Set<Post> deletePosts = new HashSet<>();
        Stack<Post> postStack = new Stack<>();
        postStack.add(post);

        while (postStack.size() > 0) {
            boolean flag = false;
            Post cur = postStack.peek();
            for (Post temp: allPosts) {
                if (temp.getReplyId() == cur.getApprove() && !deletePosts.contains(temp)) {
                    postStack.push(temp);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                Post addToSet = postStack.pop();
                deletePosts.add(addToSet);
                for (Post temp: allPosts) {
                    if (cur.getReplyId() == temp.getApprove() && !postStack.contains(temp)) {
                        postStack.push(temp);
                    }
                }
            }
        }

        List<Long> deleteIds = deletePosts.stream().map(tempPost -> tempPost.getId()).collect(Collectors.toList());
        postRepository.deleteAllById(deleteIds);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

}
