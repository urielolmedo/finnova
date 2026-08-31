package ar.edu.usal.finnova.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransaccion tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // CU-014: comprobante adjunto
    private String comprobanteUrl;
    private String comprobanteTipo; // JPG, PNG o PDF

    // CU-015: recurrencia
    @Column(nullable = false)
    private boolean esRecurrente = false;

    @Enumerated(EnumType.STRING)
    private FrecuenciaRecurrencia frecuencia;

    private LocalDate fechaFinRecurrencia;

    // Si esta transaccion es una instancia generada por una recurrencia,
    // apunta a la transaccion "original" que la configuro. Null si es una
    // transaccion normal o si ES la original de una serie recurrente.
    @ManyToOne
    @JoinColumn(name = "transaccion_origen_id")
    private Transaccion transaccionOrigen;
}