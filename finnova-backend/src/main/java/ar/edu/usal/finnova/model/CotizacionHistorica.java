package ar.edu.usal.finnova.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotizaciones_historicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionHistorica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCotizacion tipo;

    @Column(nullable = false)
    private BigDecimal compra;

    @Column(nullable = false)
    private BigDecimal venta;

    @Column(nullable = false)
    private LocalDateTime fechaHora;
}