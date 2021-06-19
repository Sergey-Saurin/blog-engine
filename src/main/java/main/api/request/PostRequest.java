package main.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostRequest
{
    @JsonProperty("timestamp")
    private Date time;
    @JsonProperty("active")
    private short isActive;
    private String title;
    private String text;
    private List<String> tags;

}
