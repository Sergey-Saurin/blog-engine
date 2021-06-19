package main.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Post {

    private int id;
    private long timestamp;
    private boolean active;
    private AuthorUser user;
    @Data
    public class AuthorUser {
        private int id;
        private String name;
    }
    private String title;
    private String text;
    private int likeCount;
    private int dislikeCount;
    private int viewCount;
    private List<Comment> comments;
    @Data
    public class Comment {
        private int id;
        private long timestamp;
        private String text;
        private User user;
        @Data
        public class User {
            private int id;
            private String name;
            private String photo;
        }
    }
    private List<String> tags;
}

