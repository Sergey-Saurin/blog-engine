package main.model;

import lombok.Data;
import main.enums.ModerationStatus;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "posts")
public class Post
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "is_active")
    private short isActive;
    @Column(name = "moderation_status")
    @Enumerated(EnumType.STRING)
    private ModerationStatus moderationStatus;
    @OneToOne
    @JoinColumn(name="moderator_id")
    private User moderator;
    @OneToOne
    @JoinColumn(name="user_id")
    private User user;
    private Date time;
    private String title;
    private String text;
    @Column(name = "view_count")
    private int viewCount;


    public boolean getIsActive() {
        return isActive  == 1;
    }

}
