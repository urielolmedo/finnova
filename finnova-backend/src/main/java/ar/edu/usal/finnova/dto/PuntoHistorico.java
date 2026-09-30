package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PuntoHistorico {
    private LocalDateTime fechaHora;
    private BigDecimal oficial;
    private BigDecimal blue;
}