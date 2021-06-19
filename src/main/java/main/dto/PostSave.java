package main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class PostSave
{
    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ErrorSavePost errors;
    @Data
    public class ErrorSavePost
    {
        private String title = "Заголовок не установлен";
        private String text = "Текст публикации слишком короткий";
    }
}
