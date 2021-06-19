package main.dto;

import lombok.Data;

import java.util.List;

@Data
public class Posts
{
    private long count;
    private List<Post> posts;
    @Data
    public class Post{
        private int id;
        private long timestamp;
        private User user;
        @Data
        public class User{
            private int id;
            private String name;
        }
        private String title;
        private String announce;
        private int likeCount;
        private int dislikeCount;
        private int commentCount;
        private int viewCount;
    }

}
