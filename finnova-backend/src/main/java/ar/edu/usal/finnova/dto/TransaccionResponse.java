package ar.edu.usal.finnova.dto;

import ar.edu.usal.finnova.model.FrecuenciaRecurrencia;
import ar.edu.usal.finnova.model.Transaccion;
import ar.edu.usal.finnova.model.TipoTransaccion;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class TransaccionResponse {
    private final Long id;
    private final TipoTransaccion tipo;
    private final BigDecimal monto;
    private final LocalDate fecha;
    private final Long categoriaId;
    private final String categoriaNombre;
    private final String descripcion;
    private final boolean tieneComprobante;
    private final String comprobanteUrl;
    private final boolean esRecurrente;
    private final FrecuenciaRecurrencia frecuencia;
    private final LocalDate fechaFinRecurrencia;

    public TransaccionResponse(Transaccion t) {
        this.id = t.getId();
        this.tipo = t.getTipo();
        this.monto = t.getMonto();
        this.fecha = t.getFecha();
        this.categoriaId = t.getCategoria().getId();
        this.categoriaNombre = t.getCategoria().getNombre();
        this.descripcion = t.getDescripcion();
        this.tieneComprobante = t.getComprobanteUrl() != null;
         this.comprobanteUrl = t.getComprobanteUrl();
        this.esRecurrente = t.isEsRecurrente();
        this.frecuencia = t.getFrecuencia();
        this.fechaFinRecurrencia = t.getFechaFinRecurrencia();
    }
}
