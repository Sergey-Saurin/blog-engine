package main.dto;

import lombok.Data;

@Data
public class CommentError
{
    private boolean result;
    private Error errors;
    @Data
    public class Error {
        private String text = "Текст комментария не задан или слишком короткий";
    }

}

