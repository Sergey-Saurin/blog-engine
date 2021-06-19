package main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class RestorePassword
{
    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Error errors;
    @Data
    public class Error{
        private String code = "Ссылка для восстановления пароля устарела.<a href=\"/auth/restore\">Запросить ссылку снова</a>," +
                "password\": \"Пароль короче 6-ти символов\",\"captcha\": \"Код с картинки введён неверно";
    }
}
