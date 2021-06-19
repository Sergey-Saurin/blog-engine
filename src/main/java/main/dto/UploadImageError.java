package main.dto;

import lombok.Data;

@Data
public class UploadImageError
{
    private boolean result;
    private Error errors;
    @Data
    public class Error
    {
        private String image = "Размер файла превышает допустимый размер";
    }
}
