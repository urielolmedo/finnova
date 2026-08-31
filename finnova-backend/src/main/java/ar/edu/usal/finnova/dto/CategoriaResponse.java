package ar.edu.usal.finnova.dto;

import ar.edu.usal.finnova.model.Categoria;
import ar.edu.usal.finnova.model.TipoCategoria;
import lombok.Getter;

@Getter
public class CategoriaResponse {
    private final Long id;
    private final String nombre;
    private final TipoCategoria tipo;
    private final boolean predefinida;

    public CategoriaResponse(Categoria c) {
        this.id = c.getId();
        this.nombre = c.getNombre();
        this.tipo = c.getTipo();
        this.predefinida = c.isPredefinida();
    }
}