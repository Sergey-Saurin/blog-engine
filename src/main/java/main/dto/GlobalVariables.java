package main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GlobalVariables
{
    @JsonProperty("MULTIUSER_MODE")
    private boolean multiuserMode;
    @JsonProperty("POST_PREMODERATION")
    private boolean postPremoderation;
    @JsonProperty("STATISTICS_IS_PUBLIC")
    private boolean statisticsIsPublic;

}
