package ar.edu.usal.finnova.service;

import ar.edu.usal.finnova.dto.CategoriaResumen;
import ar.edu.usal.finnova.dto.EvolucionMensualResponse;
import ar.edu.usal.finnova.dto.ResumenFinancieroResponse;
import ar.edu.usal.finnova.model.TipoTransaccion;
import ar.edu.usal.finnova.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final TransaccionRepository transaccionRepository;

    public ResumenFinancieroResponse calcularResumen(Long usuarioId, LocalDate desde, LocalDate hasta) {
        BigDecimal ingresos = transaccionRepository.sumarPorTipoYPeriodo(usuarioId, TipoTransaccion.INGRESO, desde, hasta);
        BigDecimal egresos = transaccionRepository.sumarPorTipoYPeriodo(usuarioId, TipoTransaccion.EGRESO, desde, hasta);
        BigDecimal balance = ingresos.subtract(egresos);
        return new ResumenFinancieroResponse(ingresos, egresos, balance, balance.signum() >= 0);
    }

    public List<CategoriaResumen> calcularPorCategoria(Long usuarioId, LocalDate desde, LocalDate hasta) {
        List<Object[]> filas = transaccionRepository.sumarAgrupadoPorCategoria(usuarioId, desde, hasta);

        // Agrupamos por nombre de categoria, porque la consulta trae una fila por (categoria, tipo)
        java.util.Map<String, CategoriaResumen> acumulado = new java.util.LinkedHashMap<>();
        for (Object[] fila : filas) {
            String nombre = (String) fila[0];
            String color = (String) fila[1];
            TipoTransaccion tipo = (TipoTransaccion) fila[2];
            BigDecimal total = (BigDecimal) fila[3];

            CategoriaResumen actual = acumulado.get(nombre);
            if (actual == null) {
                actual = new CategoriaResumen(nombre, color, BigDecimal.ZERO, BigDecimal.ZERO);
                acumulado.put(nombre, actual);
            }
            if (tipo == TipoTransaccion.INGRESO) {
                actual.setTotalIngresos(total);
            } else {
                actual.setTotalEgresos(total);
            }
        }
        return new ArrayList<>(acumulado.values());
    }
    public List<EvolucionMensualResponse> calcularEvolucion(Long usuarioId, LocalDate desde, LocalDate hasta) {
        List<EvolucionMensualResponse> resultado = new ArrayList<>();
        YearMonth actual = YearMonth.from(desde);
        YearMonth limite = YearMonth.from(hasta);

        while (!actual.isAfter(limite)) {
            LocalDate inicioMes = actual.atDay(1);
            LocalDate finMes = actual.atEndOfMonth();
            ResumenFinancieroResponse resumenMes = calcularResumen(usuarioId, inicioMes, finMes);

            resultado.add(new EvolucionMensualResponse(
                    actual.toString(), // "2026-08"
                    resumenMes.getTotalIngresos(),
                    resumenMes.getTotalEgresos(),
                    resumenMes.getBalance()
            ));
            actual = actual.plusMonths(1);
        }
        return resultado;
    }
}