package com.example.Confessions.controller;

import com.example.Confessions.ConfessionsApplication;
import com.example.Confessions.exception.MaliciousPostingError;
import com.example.Confessions.exception.ResourceNotFoundException;
import com.example.Confessions.model.Post;
import com.example.Confessions.repository.PostRepository;
import com.example.Confessions.utils.SentimentAnalyzerService;
import com.example.Confessions.utils.SpamCheck;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/")
@Slf4j
public class PostController {

    // Inject repository here
    @Autowired
    private PostRepository postRepository;

    // Spam checking utils
    private SpamCheck spamCheck = new SpamCheck(10, 0.2);

    // Sentiment analysis
    private SentimentAnalyzerService analyzer = new SentimentAnalyzerService();

    // get all posts at /posts api
    @GetMapping("/posts")
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }


    // Users submit new posts
    @PostMapping("/posts")
    public ResponseEntity<?> submitPost(@RequestBody Post post) {

        // Spam checking on post content
        if (spamCheck.contentIsSpam(post.getContent())) {
            MaliciousPostingError errorResponse = new MaliciousPostingError();
            errorResponse.setMessage("Your submission has been rejected as it has been detected as a spam!");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        // Sentiment analyzer
        if (analyzer.analyse(post.getContent()) <= 1) {
            MaliciousPostingError errorResponse = new MaliciousPostingError();
            errorResponse.setMessage("Your post has been rejected as it contains elements of vulgarity or depression!");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        // Comparative spam checking
        List<Post> allPosts = postRepository.findAll();
        int results = spamCheck.comparativeSpamCheck(post, allPosts);

        switch (results) {
            case -1:
                MaliciousPostingError errorResponse = new MaliciousPostingError();
                errorResponse.setMessage("Something went wrong!");
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            case 0:
                MaliciousPostingError replyIdError = new MaliciousPostingError();
                replyIdError.setMessage("Your post has been rejected as the reply Id does not exist!");
                return new ResponseEntity<>(replyIdError, HttpStatus.NOT_FOUND);
        }

        // Add to pending queue
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("content", post.getContent());
        jsonObject.put("replyId", post.getReplyId());
        jsonObject.put("submitId", post.getSubmitId());
        System.out.println(post.getDatePosted().toString());
        jsonObject.put("datePosted", post.getDatePosted().toString());
        jsonObject.put("hasFile", post.getHasFile());
        ConfessionsApplication.postQueue.offer(jsonObject);
        ConfessionsApplication.updateQueue();

        return ResponseEntity.ok().build();
    }


    // Publish post
    @PostMapping("/posts/{submitId}")
    public ResponseEntity.BodyBuilder publishPost(@PathVariable Long submitId) throws java.text.ParseException {

        // The casting & iterating operation is allowed because we are using LinkedList to implement queue
        List<JSONObject> tempList = (List<JSONObject>) ConfessionsApplication.postQueue;
        for (Object post: tempList) {
            JSONObject tempObj = (JSONObject) post;
            if ( (long) tempObj.get("submitId") == submitId) {
                String dateInString = (String) tempObj.get("datePosted");
                SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
                Date datePosted = formatter.parse(dateInString);
                String content = (String) tempObj.get("content");
                long replyId = (long) tempObj.get("replyId");
                long hasFile = (long) tempObj.get("hasFile");
                Post newPost = new Post(submitId, content, datePosted, replyId, hasFile);
                postRepository.save(newPost);

                tempList.remove(post);
                ConfessionsApplication.postQueue = new LinkedList<>(tempList);
                // JSON list to handle store the updated elements in the queue
                JSONArray updateList = new JSONArray();

                // Since we implemented postQueue using LinkedList,
                // we can iterate over the objects in order they were inserted
                for (Object p: ConfessionsApplication.postQueue) {
                    JSONObject postObj = (JSONObject) p;
                    updateList.add(postObj);
                }

                // Update the queue.json file with current queue
                try (FileWriter writer = new FileWriter(ConfessionsApplication.path)) {
                    writer.write(updateList.toJSONString());
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return ResponseEntity.status(HttpStatus.OK);
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND);
    }

    // get pending posts
    @GetMapping("/posts/pending")
    public List<Post> getPendingPosts() throws java.text.ParseException {

        // Return pending posts list
        List<Post> pendingList = new ArrayList<>();

        // Parse pending posts (JSON objects) in queue and convert them to post objects
        for (JSONObject jsonObject: ConfessionsApplication.postQueue) {
            String dateInString = (String) jsonObject.get("datePosted");
            // Fri Jun 24 23:53:22 MYT 2022
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
            Date datePosted = formatter.parse(dateInString);
            long submitId = (long) jsonObject.get("submitId");
            String content = (String) jsonObject.get("content");
            long replyId = (long) jsonObject.get("replyId");
            long hasFile = (long) jsonObject.get("hasFile");
            Post newPost = new Post(submitId, content, datePosted, replyId, hasFile);
            pendingList.add(newPost);
        }
        return pendingList;
    }

    // delete post api
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, Boolean>> deletePost(@PathVariable Long id) {

        // Find post with the id, if exists, delete it else return error
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));
        postRepository.delete(post);
        Map<String, Boolean> response = new HashMap<>();
        response.put("Deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    // delete posts by batch
    @DeleteMapping("/posts/{id}/_batch")
    public ResponseEntity<Map<String, Boolean>> deletePostBatch(@PathVariable Long id) {

        // Find the starting post
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));

        // Find all posts from database
        List<Post> allPosts = postRepository.findAll();

        // Store ids of posts to be deleted, use set to avoid duplicates
        Set<Post> deletePosts = new HashSet<>();

        // Helper for DFS
        Stack<Post> postStack = new Stack<>();

        // Add the starting post to the stack
        postStack.add(post);

        // Batch delete operation
        while (postStack.size() > 0) {
            boolean flag = false;
            Post cur = postStack.peek();

            for (Post temp: allPosts) {
                // Search for a post that is replying to the current post and not added to the set
                if (temp.getReplyId() == cur.getId() && !deletePosts.contains(temp)) {
                    // Add the post into the stack
                    postStack.push(temp);
                    flag = true;
                    // Break to start DFS for next post (the post that is replying to the current post)
                    break;
                }
            }

            // No post is replying to the current post
            if (!flag) {
                // Add current post to the set
                Post addToSet = postStack.pop();
                deletePosts.add(addToSet);
                for (Post temp: allPosts) {
                    // Check if the current post is replying to another post
                    if (cur.getReplyId() == temp.getId() && !postStack.contains(temp)) {
                        postStack.push(temp);
                        // One post can only reply to another post
                        break;
                    }
                }
            }
        }

        // Get the ids of the posts in delete stack
        List<Long> deleteIds = deletePosts.stream().map(tempPost -> tempPost.getId()).collect(Collectors.toList());

        // Delete those posts by id
        postRepository.deleteAllById(deleteIds);

        // Return a response
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/_search/{keywords}")
    public ResponseEntity<List<Post>> searchPosts(@PathVariable String keywords) {

        List<Post> availablePosts = postRepository.findAll();
        List<Post> relatedPosts = new ArrayList<>();
        String newDateString = "";
        boolean possibleDate = keywords.length() == 10;
        boolean possibleDateTime = keywords.length() == 19;
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        // Search
        for (Post post: availablePosts) {

            // Date & date time search
            if (possibleDate) {
                try {
                    Date searchDate = dateFormatter.parse(keywords);
                    String curPostDateString = post.getDatePosted().toString().split(" ")[0];
                    Date curPostDate = dateFormatter.parse(curPostDateString);
                    if (searchDate.getTime() - curPostDate.getTime() == 0) {
                        relatedPosts.add(post);
                    }
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }
            }else if (possibleDateTime) {
                try {
                    Date searchDate = dateTimeFormatter.parse(keywords);
                    //System.out.println(searchDate);
                    System.out.println(post.getContent() + " " + post.getDatePosted());
                    if (searchDate.getTime() - post.getDatePosted().getTime() == 0) {
                        relatedPosts.add(post);
                    }
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }
            }

            // Check if the content or date of a post contains the keyword
            if (post.getContent().contains(keywords)) {
                relatedPosts.add(post);
            } else {
                try {
                    // See if the keyword is actually an id
                    Long potentialId = Long.parseLong(keywords);
                    if (potentialId == post.getId()) {
                        relatedPosts.add(post);
                    }
                } catch (NumberFormatException nfe) {
                    //System.out.println("Not ID");
                }
            }
        }
        return ResponseEntity.ok(relatedPosts);
    }

    // Get latest submit id for a new post
    @GetMapping("/posts/submitId")
    public long getSubmitId() {
        long latestSubmitId = 1;

        // Read the latest submit id
        JSONParser jsonParser = new JSONParser();

        // This file contains the last post's submit id
        try(FileReader reader = new FileReader(ConfessionsApplication.submitIdPath)) {
            Object obj = jsonParser.parse(reader);
            JSONArray submitIdList = (JSONArray) obj;

            // This will be the next post's submit id, so the current submit id is added by 1
            latestSubmitId =  (long) ( (JSONObject) submitIdList.get(0) ).get("submitId") + 1;

            // Update the latest submit id in the json file
            JSONObject newLatestSubmitId = new JSONObject();
            newLatestSubmitId.put("submitId", latestSubmitId);
            JSONArray tempList = new JSONArray();
            tempList.add(newLatestSubmitId);

            try (FileWriter writer = new FileWriter(ConfessionsApplication.submitIdPath)) {
                writer.write(tempList.toJSONString());
                writer.flush();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Return the next post's submit id
        return latestSubmitId;
    }

    // Called by application class to publish post automatically
    public ResponseEntity<?> autoPublishPost(Post post) {
        Post returnPost = postRepository.save(post);
        return ResponseEntity.ok(returnPost);
    }
    
    // get post by id {} means path variable
    @GetMapping("/posts/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));
        return ResponseEntity.ok(post);
    }
}
