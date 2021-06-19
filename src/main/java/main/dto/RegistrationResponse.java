package main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class RegistrationResponse
{
    private boolean result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Error errors;
    @Data
    public class Error {
        private String email = "Этот e-mail уже зарегистрирован";
        private String name = "Имя указано неверно";
        private String password = "Пароль короче 6-ти символов";
        private String captcha = "Код с картинки введён неверно";
    }
}
