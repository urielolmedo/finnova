package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CompartirReporteResponse {
    private String token;
    private String urlPublica;
    private String fechaExpiracion;
}