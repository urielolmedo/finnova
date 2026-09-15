package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionRespaldo {
    private String tipo;
    private BigDecimal monto;
    private LocalDate fecha;
    private String categoriaNombre;
    private String descripcion;
}