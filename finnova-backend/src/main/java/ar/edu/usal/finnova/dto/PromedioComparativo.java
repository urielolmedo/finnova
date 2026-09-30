package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PromedioComparativo {
    private int dias;
    private BigDecimal promedio;
    private BigDecimal desviacionPorcentual; // negativo = favorable (actual por debajo del promedio)
    private boolean favorable;
}