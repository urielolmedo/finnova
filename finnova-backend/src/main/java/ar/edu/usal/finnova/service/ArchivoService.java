package ar.edu.usal.finnova.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ArchivoService {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    private static final List<String> TIPOS_PERMITIDOS = List.of("image/jpeg", "image/png", "application/pdf");
    private static final long TAMANO_MAXIMO = 5 * 1024 * 1024; // 5MB

    public String guardar(MultipartFile archivo) throws IOException, IllegalArgumentException {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (!TIPOS_PERMITIDOS.contains(archivo.getContentType())) {
            throw new IllegalArgumentException("Formato no permitido. Solo se aceptan JPG, PNG o PDF");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (5MB)");
        }

        Path directorio = Paths.get(uploadsDir);
        if (!Files.exists(directorio)) {
            Files.createDirectories(directorio);
        }

        String extension = switch (archivo.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".pdf";
        };
        String nombreArchivo = UUID.randomUUID() + extension;
        Path destino = directorio.resolve(nombreArchivo);
        Files.copy(archivo.getInputStream(), destino);

        return nombreArchivo;
    }

    public void eliminar(String nombreArchivo) {
        try {
            Path path = Paths.get(uploadsDir).resolve(nombreArchivo);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // No bloqueamos la operacion principal si falla el borrado del archivo fisico
        }
    }
}