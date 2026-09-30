package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class SimulacionResponse {
    private BigDecimal montoPesos;
    private String tipoCotizacion;
    private BigDecimal equivalenteActual;
    private List<EquivalenteHistorico> equivalentesHistoricos;
    private String aviso;
    private boolean cotizacionDesactualizada;
}