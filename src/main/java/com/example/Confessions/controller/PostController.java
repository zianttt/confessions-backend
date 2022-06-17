package com.example.Confessions.controller;

import com.example.Confessions.ConfessionsApplication;
import com.example.Confessions.exception.MaliciousPostingError;
import com.example.Confessions.exception.ResourceNotFoundException;
import com.example.Confessions.model.Post;
import com.example.Confessions.repository.PostRepository;
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

    // get all posts at /posts api
    @GetMapping("/posts")
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // Users submit new posts
    @PostMapping("/posts")
    public ResponseEntity<?> submitPost(@RequestBody Post post) {

        if (post.getContent().isEmpty()) {
            MaliciousPostingError errorResponse = new MaliciousPostingError();
            errorResponse.setMessage("Spamming Detected!");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
        // Spam checking
        int occurance = 0;
        boolean replyIdExists = false;
        List<Post> allPosts = postRepository.findAll();
        for (Post curPost: allPosts) {
            if (curPost.getContent().equalsIgnoreCase(post.getContent()))
                occurance++;
            if (post.getReplyId() == curPost.getId()) {
                replyIdExists = true;
            }
        }
        if (occurance >= 3) {
            MaliciousPostingError errorResponse = new MaliciousPostingError();
            errorResponse.setMessage("Spamming Detected!");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
        if(!replyIdExists && post.getReplyId() != -1) {
            MaliciousPostingError errorResponse = new MaliciousPostingError();
            errorResponse.setMessage("Reply Id not exist!");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("content", post.getContent());
        jsonObject.put("replyId", post.getReplyId());
        jsonObject.put("submitId", post.getSubmitId());
        jsonObject.put("datePosted", post.getDatePosted().toString());
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
                Post newPost = new Post(submitId, content, datePosted, replyId);
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

    // get all posts at /posts api
    @GetMapping("/posts/pending")
    public List<Post> getPendingPosts() throws java.text.ParseException {
        List<Post> pendingList = new ArrayList<>();

        for (JSONObject jsonObject: ConfessionsApplication.postQueue) {
            String dateInString = (String) jsonObject.get("datePosted");
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
            Date datePosted = formatter.parse(dateInString);
            long submitId = (long) jsonObject.get("submitId");
            String content = (String) jsonObject.get("content");
            long replyId = (long) jsonObject.get("replyId");
            pendingList.add(new Post(submitId, content, datePosted, replyId));
        }
        return pendingList;
    }

    // delete post api
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, Boolean>> deletePost(@PathVariable Long id) {
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
        //List<Long> postIdsList = Arrays.asList(postIds.split(" ")).stream().map(Long::parseLong).collect(Collectors.toList());
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found for ID: " + id));

        List<Post> allPosts = postRepository.findAll();
        Set<Post> deletePosts = new HashSet<>();
        Stack<Post> postStack = new Stack<>();
        postStack.add(post);

        // batch delete method
        while (postStack.size() > 0) {
            boolean flag = false;
            Post cur = postStack.peek();
            for (Post temp: allPosts) {
                if (temp.getReplyId() == cur.getId() && !deletePosts.contains(temp)) {
                    postStack.push(temp);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                Post addToSet = postStack.pop();
                deletePosts.add(addToSet);
                for (Post temp: allPosts) {
                    if (cur.getReplyId() == temp.getId() && !postStack.contains(temp)) {
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

    @GetMapping("/posts/_search/{keywords}")
    public ResponseEntity<List<Post>> searchPosts(@PathVariable String keywords) {

        List<Post> availablePosts = postRepository.findAll();
        List<Post> relatedPosts = new ArrayList<>();


        // Search
        for (Post post: availablePosts) {
            if (post.getContent().contains(keywords)
                    || post.getDatePosted().toString().split(" ")[0].equalsIgnoreCase(keywords)) {
                relatedPosts.add(post);
            } else {
                try {
                    Long potentialId = Long.parseLong(keywords);
                    if (potentialId == post.getId()) {
                        relatedPosts.add(post);
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("Not ID");
                }

            }
        }
        return ResponseEntity.ok(relatedPosts);
    }

    @GetMapping("/posts/submitId")
    public long getSubmitId() {
        long latestSubmitId = 1;

        // Read the latest submit id
        JSONParser jsonParser = new JSONParser();
        try(FileReader reader = new FileReader(ConfessionsApplication.submitIdPath)) {
            Object obj = jsonParser.parse(reader);
            JSONArray submitIdList = (JSONArray) obj;

            // This will be the next post's submit id, so the current submit id is added by 1
            latestSubmitId =  (long) ( (JSONObject) submitIdList.get(0) ).get("submitId") + 1;

            JSONObject newLatestSubmitId = new JSONObject();
            newLatestSubmitId.put("submitId", latestSubmitId);
            JSONArray tempList = new JSONArray();
            tempList.add(newLatestSubmitId);

            // Update the latest submit id in the json file
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
