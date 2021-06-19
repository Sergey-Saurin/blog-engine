package main.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "post_comments")
public class PostComment
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToOne
    @JoinColumn(name="parent_id")
    private PostComment parentComment;
    @OneToOne
    @JoinColumn(name="post_id")
    private Post post;
    @OneToOne
    @JoinColumn(name="user_id")
    private User user;
    private Date time;
    private String text;


}
