package ar.edu.usal.finnova.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ImportacionPreviewResponse {
    private int cantidadValidas;
    private List<FilaImportacion> filasValidas;
    private List<String> errores; // ej: "Fila 5: monto invalido"
}