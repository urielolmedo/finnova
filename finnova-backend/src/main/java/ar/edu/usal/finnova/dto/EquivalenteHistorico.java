package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class EquivalenteHistorico {
    private int dias;
    private BigDecimal equivalente;
    private BigDecimal diferenciaPorcentual;
}