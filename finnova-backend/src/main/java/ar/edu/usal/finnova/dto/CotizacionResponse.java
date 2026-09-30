package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CotizacionResponse {
    private String tipo;
    private BigDecimal compra;
    private BigDecimal venta;
    private LocalDateTime fechaHora;
    private boolean desactualizada; // true si viene del mecanismo de contingencia (API caida)
}