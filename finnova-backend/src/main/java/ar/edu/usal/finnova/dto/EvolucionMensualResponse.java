package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class EvolucionMensualResponse {
    private String periodo; // formato "2026-08"
    private BigDecimal ingresos;
    private BigDecimal egresos;
    private BigDecimal balance;
}