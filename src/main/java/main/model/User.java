package main.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import main.enums.Role;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Data
@Table(name = "users")
public class User implements Serializable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "is_moderator")
    private short isModerator;
    @Column(name = "reg_time")
    private Date regTime;
    private String name;
    @JsonProperty("e_mail")
    private String email;
    private String password;
    private String code;
    private String photo;

    public boolean getIsModerator() {
        return isModerator == 1;
    }

    public Role getUserRole()
    {
        return isModerator == 1 ? Role.MODERATOR : Role.USER;
    }

}
