package main.model;

import lombok.Data;
import main.model.Post;
import main.model.Tag;

import javax.persistence.*;

@Entity
@Data
@Table(name = "tag2post")
public class Tag2Post
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToOne
    @JoinColumn(name="post_id")
    private Post post;
    @OneToOne
    @JoinColumn(name="tag_id")
    private Tag tag;


}
