package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ResumenFinancieroResponse {
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal balance;
    private boolean superavit; // true = balance >= 0
}