package ar.edu.usal.finnova.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetearPasswordRequest {
    private String token;
    private String nuevaPassword;
}