package ar.edu.usal.finnova.dto;

import ar.edu.usal.finnova.model.TipoCategoria;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaRequest {
    private String nombre;
    private TipoCategoria tipo;
    private String color;
}