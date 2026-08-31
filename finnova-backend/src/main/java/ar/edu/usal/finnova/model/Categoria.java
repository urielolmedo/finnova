package ar.edu.usal.finnova.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCategoria tipo;

    // true = predefinida por el sistema, false = creada por un usuario (Modulo 3, Sprint 3)
    @Column(nullable = false)
    private boolean predefinida = true;

    // null si es predefinida; si en el futuro un usuario crea la suya propia, se asocia aca
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}