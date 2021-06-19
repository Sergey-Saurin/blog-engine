package main.dto;

import lombok.Data;

@Data
public class Statistics {
    private Long postsCount;
    private Long likesCount;
    private Long dislikesCount;
    private Long viewsCount;
    private Long firstPublication;

}
