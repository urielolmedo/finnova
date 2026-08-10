package ar.edu.usal.finnova.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroRequest {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
}