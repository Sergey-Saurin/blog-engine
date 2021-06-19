package main.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "global_settings")
public class GlobalVariable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String code;
    private String name;
    private String value;

}
