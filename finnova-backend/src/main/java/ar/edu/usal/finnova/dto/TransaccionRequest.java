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

    // CU-015: recurrencia (opcionales, solo si esRecurrente = true)
    private boolean esRecurrente = false;
    private FrecuenciaRecurrencia frecuencia;
    private LocalDate fechaFinRecurrencia;
}
