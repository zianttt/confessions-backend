package com.example.Confessions;

import com.example.Confessions.controller.PostController;
import com.example.Confessions.model.Post;

import java.io.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class ConfessionsApplication {

	public final static String path = "./src/main/resources/postsQueue/queue.json";
	public final static String submitIdPath = "./src/main/resources/postsQueue/submitId.json";
	public static Queue<JSONObject> postQueue;

	@Autowired
	private PostController postController;

	public static void main(String[] args) {

		JSONParser jsonParser = new JSONParser();
		try(FileReader reader = new FileReader(path)) {
			Object obj = jsonParser.parse(reader);
			JSONArray postList = (JSONArray) obj;
			postQueue = new LinkedList<>(postList);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		SpringApplication.run(ConfessionsApplication.class, args);
	}

	// Check every 5 minutes (300000 ms)
	@Scheduled(fixedDelay = 20000)
	public void checkQueue() throws java.text.ParseException {

		if (postQueue.isEmpty()) return;

		// Remove posts that queued for at least 5 minutes
		if (postQueue.size() > 10) {
			// Parse exception for popQualifiedPosts
			popQualifiedPosts(5);
		}
		// Remove posts that queued for at least 10 minutes
		else if (postQueue.size() > 5) {
			popQualifiedPosts(10);
		} else {
			popQualifiedPosts(15);
		}
		updateQueue();
	}

	public static void updateQueue() {
		// JSON list to handle store the updated elements in the queue
		JSONArray updateList = new JSONArray();

		// Since we implemented postQueue using LinkedList,
		// we can iterate over the objects in order they were inserted
		for (JSONObject post: postQueue) {
			updateList.add(post);
		}

		// Update the queue.json file with current queue
		try (FileWriter writer = new FileWriter(ConfessionsApplication.path)) {
			writer.write(updateList.toJSONString());
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	// Get the post date of a post (JSONObject)
	private static Date getPostDate(JSONObject post) throws java.text.ParseException {
		String dateInString = (String) post.get("datePosted");
		SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a");
		Date datePosted = formatter.parse(dateInString);
		return datePosted;
 	}

	 // Get difference between 2 dates
	private static long getDateDiff(Long date1, Long date2, TimeUnit timeUnit) {
		long diffInMillies = date1 - date2;
		long difMins = diffInMillies / 1000 / 60;
		System.out.println(difMins);
		return difMins;

	}

	// Pop posts that exist in queue for a given duration
	private void popQualifiedPosts(int duration) throws java.text.ParseException {

		while (!postQueue.isEmpty()) {

			Long currentDate = System.currentTimeMillis();

			JSONObject post = postQueue.peek();
			// Parse exception
			String dateInString = (String) post.get("datePosted");
			SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
			Date datePosted = formatter.parse(dateInString);

			// Since the queue is ordered by date, when we found a post
			// that exist no longer than the required duration, we can straight away stop the operation
			if (getDateDiff(currentDate, datePosted.getTime(), TimeUnit.MINUTES) < duration) {
				break;
			}

			// Pop qualified post from the queue and add to database
			JSONObject postObj =  postQueue.poll();
			long submitId = (long) postObj.get("submitId");
			String content = (String) postObj.get("content");
			long replyId = (long) postObj.get("replyId");
			long hasFile = (long) postObj.get("hasFile");
			Post newPost = new Post(submitId, content, datePosted, replyId, hasFile);

			// Method to upload post to database
			postController.autoPublishPost(newPost);
		}
	}
}
