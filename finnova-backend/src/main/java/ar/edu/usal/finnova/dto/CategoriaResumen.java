package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class CategoriaResumen {
    private String categoriaNombre;
    private String color;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
}