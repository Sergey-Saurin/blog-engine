package main.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

@Data
public class PostsByDate
{
    private List<Integer> years;
    private TreeMap<String, Long> posts;

}

