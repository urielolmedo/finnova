package ar.edu.usal.finnova.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SimulacionRequest {
    private BigDecimal montoPesos;
    private String tipoCotizacion; // "oficial" o "blue"
}