package main.dto;

import lombok.Data;

@Data
public class Captcha {
    private String secret;
    private String image;
}
