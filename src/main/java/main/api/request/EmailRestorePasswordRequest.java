package main.api.request;

import lombok.Data;

@Data
public class EmailRestorePasswordRequest
{
    private String email;
}
