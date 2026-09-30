package ar.edu.usal.finnova.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CotizacionApiResponse {
    private BigDecimal compra;
    private BigDecimal venta;
    private String casa;
    private String nombre;
    private String moneda;
    private String fechaActualizacion;
}