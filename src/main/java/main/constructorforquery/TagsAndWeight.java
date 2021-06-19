package main.constructorforquery;

import lombok.Data;

@Data
public class TagsAndWeight
{
    private String name;
    private double quantityPostByTag;
    private double quantityAllPost;

    public TagsAndWeight(String name, long quantityPostByTag, long quantityAllPost) {
        this.name = name;
        this.quantityPostByTag = quantityPostByTag;
        this.quantityAllPost = quantityAllPost;
    }
}
