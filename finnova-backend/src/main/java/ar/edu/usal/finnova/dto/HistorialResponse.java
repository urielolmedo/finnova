package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class HistorialResponse {
    private List<PuntoHistorico> puntos;
    private BigDecimal minOficial;
    private BigDecimal maxOficial;
    private BigDecimal promedioOficial;
    private BigDecimal minBlue;
    private BigDecimal maxBlue;
    private BigDecimal promedioBlue;
    private LocalDate primerRegistroDisponible; // solo se completa si el historial es menor al rango pedido
}