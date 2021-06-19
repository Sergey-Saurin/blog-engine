package main.dto;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Tags
{
    private ArrayList<weightTag> tags;
    @Data
    public class weightTag{
        private String name;
        private String weight;
    }
}
