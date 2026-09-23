package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReporteMensualResponse {
    private int anio;
    private int mes;
    private ResumenFinancieroResponse resumen;
    private List<CategoriaResumen> porCategoria;
}