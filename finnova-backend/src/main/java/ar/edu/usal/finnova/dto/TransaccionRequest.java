package ar.edu.usal.finnova.dto;

import ar.edu.usal.finnova.model.FrecuenciaRecurrencia;
import ar.edu.usal.finnova.model.TipoTransaccion;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TransaccionRequest {
    private TipoTransaccion tipo;
    private BigDecimal monto;
    private LocalDate fecha;
    private Long categoriaId;
    private String descripcion;

    // CU-015: recurrencia. El usuario elige UNO de los dos modos:
    private boolean esRecurrente = false;
    private FrecuenciaRecurrencia frecuencia;
    private Integer cantidadRepeticiones;      // modo 1: "repetir N veces"
    private LocalDate fechaFinRecurrencia;     // modo 2: "repetir hasta tal fecha"
}