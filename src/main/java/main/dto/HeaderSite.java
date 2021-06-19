package main.dto;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "head.information")
public class HeaderSite
{
    private String title;
    private String subtitle;
    private String phone;
    private String email;
    private String copyright;
    private String copyrightFrom;

}
