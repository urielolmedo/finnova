package ar.edu.usal.finnova.dto;

import ar.edu.usal.finnova.model.Usuario;
import lombok.Getter;

@Getter
public class UsuarioResponse {
    private final Long id;
    private final String email;
    private final String nombre;
    private final String apellido;
    private final String modulosActivos;

    public UsuarioResponse(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.nombre = usuario.getNombre();
        this.apellido = usuario.getApellido();
        this.modulosActivos = usuario.getModulosActivos();
    }
}