package main.constructorforquery;

import lombok.Data;

import java.util.Date;

@Data
public class DateWithQuantityPosts
{
    private Date date;
    private long quantity;

    public DateWithQuantityPosts(Date date, long quantity) {
        this.date = date;
        this.quantity = quantity;
    }
}
