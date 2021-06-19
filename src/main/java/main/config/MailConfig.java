package main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "mail")
public class MailConfig {

    private String protocol;
    private String smtpHost;
    private String smtpPort;
    private String smtpSslEnable;
    private String smtpAuth;
    private String debug;
    private String login;
    private String password;
}
