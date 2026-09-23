package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class ReportePersonalizadoResponse {
    private LocalDate desde;
    private LocalDate hasta;
    private ResumenFinancieroResponse resumen;
    private List<CategoriaResumen> porCategoria;
}

