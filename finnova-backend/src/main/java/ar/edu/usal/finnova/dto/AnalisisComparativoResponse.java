package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class AnalisisComparativoResponse {
    private String tipo;
    private BigDecimal cotizacionActual;
    private List<PromedioComparativo> comparativas; // 30, 60 y 90 dias
    private Integer diasHistorialDisponible; // se completa solo si es menor a 30
}