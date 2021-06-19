package main.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "post_votes")
public class PostVote
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToOne
    @JoinColumn(name="user_id")
    private User user;
    @OneToOne
    @JoinColumn(name="post_id")
    private Post post;
    private Date time;
    private short value;
}
