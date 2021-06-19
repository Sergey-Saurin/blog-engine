package main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private User user;
    @Data
    public class User
    {
        private int id;
        private String name;
        private String photo;
        private String email;
        private boolean moderation;
        private int moderationCount;
        private boolean settings;
    }
}
