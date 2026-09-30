package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SugerenciaResponse {
    private String mensaje;
    private String criterio;
    private String aviso;
    private Integer diasHistorialDisponible; // solo se completa si hay menos de 30 dias
}