package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RestauracionPreviewResponse {
    private String fechaGeneracionRespaldo;
    private int cantidadCategorias;
    private int cantidadTransacciones;
    private RespaldoCompleto respaldo; // se reenvia tal cual al confirmar (sin estado en el servidor)
}