package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RespaldoCompleto {
    private String version;
    private LocalDateTime fechaGeneracion;
    private String usuarioEmail;
    private String modulosActivos;
    private List<CategoriaRespaldo> categoriasPersonalizadas;
    private List<TransaccionRespaldo> transacciones;
}