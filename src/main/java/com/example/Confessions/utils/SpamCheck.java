package com.example.Confessions.utils;

import com.example.Confessions.exception.MaliciousPostingError;
import com.example.Confessions.model.Post;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class SpamCheck {

    private String[] triggers = {"$$$" ,"100% free", "apply now", "Earn $", "earn extra cash", "free offer", "online marketing",
            "life insurance", "limited time offer", "1-800", "1-888"};
    private int minLength = 8;
    private double threshold = 0.2;

    public SpamCheck() {

    }

    public SpamCheck(int minLength, double threshold) {
        this.minLength = minLength;
        this.threshold = threshold;
    }

    // Check for spam by inspecting the newContent itself
    public boolean contentIsSpam(String newContent) {

        // Empty content
        if (newContent.isEmpty()) {
            return true;
        }

        // check for no. of symbols against no. of words
        int numOfInvalid = 0;
        int numOfChecked = newContent.length();
        for (int i = 0; i < newContent.length(); i++) {
            char c = newContent.charAt(i);
            if (Character.isWhitespace(c)) {
                numOfChecked--; // We don't count whitespaces.
            } else if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
                numOfInvalid++;
            } else {
                numOfInvalid += Character.isLetterOrDigit(c) ? 0 : 1;
            }
        }

        return numOfChecked > 0 && numOfChecked >= minLength &&
                (double)numOfInvalid / (double)numOfChecked >= threshold ||
                duplicate(newContent) ||
                keySmash(newContent) || spamTrigger(newContent);
    }

    // Check for spam by comparing newContent to existing posts
    // return -1 for spam; 0 for id not exists; 1 for no spam detected
    public int comparativeSpamCheck(Post post, List<Post> existingPosts) {
        // Check if
        int flags = 0;
        boolean replyIdExists = false;

        for (Post curPost: existingPosts) {
            if (findSimilarity(curPost.getContent(), (post.getContent())) >=0.7)
                flags++;
            if (post.getReplyId() == curPost.getId()) {
                replyIdExists = true;
            }
        }
        if (flags > 3) {
            return -1;
        }
        // Reply id does not exist
        if(!replyIdExists && post.getReplyId() != -1) {
            MaliciousPostingError errorResponse = new MaliciousPostingError();
            errorResponse.setMessage("Reply Id not exist!");
            return 0;
        }
        return 1;
    }


    //prevent repeating words
    public boolean duplicate(String message){

        String words[] = message.split(" ");

        for(int i = 0; i < words.length; i++) {
            int count = 1;
            for(int j = i+1; j < words.length; j++) {
                if(words[i].equals(words[j])) {
                    count++;
                    words[j] = "0";
                }
            }
            if(count > 4)
                return true;
        }
        return false;
    }

    public boolean keySmash(String message){
        String words[] = message.split(" ");

        //keySmash with space
        for (int i=0; i<words.length; i++) {
            if (words[i].length() > 10) {
                return true;
            }
        }

        //keySmash without space
        if (!message.contains(" ") && message.length()>9)
            return true;

        return false;
    }

    public boolean spamTrigger(String message){

        for(int i=0; i < triggers.length; i++){
            if(message.toLowerCase().contains(triggers[i].toLowerCase()))
                return true;
        }

        return false;
    }

    //find similarity percent between message and preMessage
    public int getLevenshteinDistance(String X, String Y){
        int m = X.length();
        int n = Y.length();

        int[][] T = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            T[i][0] = i;
        }
        for (int j = 1; j <= n; j++) {
            T[0][j] = j;
        }

        int cost;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                cost = X.charAt(i - 1) == Y.charAt(j - 1) ? 0: 1;
                T[i][j] = Integer.min(Integer.min(T[i - 1][j] + 1, T[i][j - 1] + 1),
                        T[i - 1][j - 1] + cost);
            }
        }

        return T[m][n];
    }

    public double findSimilarity(String newContent, String postedContent) {

        double maxLength = Double.max(newContent.length(), postedContent.length());
        if (maxLength > 0) {
            // optionally ignore case if needed
            return (maxLength - getLevenshteinDistance(newContent, postedContent)) / maxLength;
        }
        return 1.0;
    }
}
