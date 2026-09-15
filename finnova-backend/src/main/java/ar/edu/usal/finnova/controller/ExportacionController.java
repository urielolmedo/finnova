package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.model.Transaccion;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.TransaccionRepository;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ar.edu.usal.finnova.dto.FilaImportacion;
import ar.edu.usal.finnova.dto.ImportacionPreviewResponse;
import ar.edu.usal.finnova.model.Categoria;
import ar.edu.usal.finnova.model.TipoTransaccion;
import ar.edu.usal.finnova.repository.CategoriaRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import ar.edu.usal.finnova.dto.CategoriaRespaldo;
import ar.edu.usal.finnova.dto.RespaldoCompleto;
import ar.edu.usal.finnova.dto.RestauracionPreviewResponse;
import ar.edu.usal.finnova.dto.TransaccionRespaldo;
import ar.edu.usal.finnova.model.TipoCategoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/exportar")
@RequiredArgsConstructor
public class ExportacionController {

    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    }
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,              // 2027-06-15 (nuestro formato de exportacion)
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),      // 06-15-2027
            DateTimeFormatter.ofPattern("MM-dd-yy"),        // 06-15-27 (formato regional en ingles)
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),      // 15/06/2027 (formato regional en español)
            DateTimeFormatter.ofPattern("dd-MM-yyyy")       // 15-06-2027
    );

    private LocalDate parsearFecha(String texto) {
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException ignored) {
                // probamos el siguiente formato
            }
        }
        throw new DateTimeParseException("Formato de fecha no reconocido", texto, 0);
    }

    // CU-070: Exportar datos financieros en CSV
    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            Authentication authentication) {

        Usuario usuario = usuarioActual(authentication);

        List<Transaccion> transacciones;
        if (desde != null && hasta != null) {
            transacciones = transaccionRepository.findByUsuarioIdAndFechaBetweenOrderByFechaDesc(usuario.getId(), desde, hasta);
        } else {
            transacciones = transaccionRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId());
        }

        if (transacciones.isEmpty()) {
            return ResponseEntity.badRequest().body("No hay transacciones disponibles para el período seleccionado".getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder csv = new StringBuilder();
        // Punto y coma como separador, para compatibilidad con Excel en español (nota del CdU)
        csv.append("ID;Tipo;Monto;Fecha;Categoria;Descripcion;TieneComprobante\n");
        for (Transaccion t : transacciones) {
            csv.append(t.getId()).append(';')
                    .append(t.getTipo()).append(';')
                    .append(t.getMonto()).append(';')
                    .append(t.getFecha()).append(';')
                    .append(t.getCategoria().getNombre()).append(';')
                    .append(t.getDescripcion() != null ? t.getDescripcion().replace(";", ",") : "").append(';')
                    .append(t.getComprobanteUrl() != null ? "Si" : "No")
                    .append('\n');
        }

        String fechaGeneracion = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String nombreArchivo = "FinNova_Transacciones_" + fechaGeneracion + ".csv";

        // BOM UTF-8 al principio: sin esto, Excel puede mostrar mal las tildes (ñ, á, é...)
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[bom.length + contenido.length];
        System.arraycopy(bom, 0, resultado, 0, bom.length);
        System.arraycopy(contenido, 0, resultado, bom.length, contenido.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resultado);
    }

    // CU-071 (paso 1): Vista previa de la importacion, valida sin guardar nada
    @PostMapping(value = "/importar/csv/preview", consumes = "multipart/form-data")
    public ResponseEntity<?> previsualizarImportacion(
            @org.springframework.web.bind.annotation.RequestParam("archivo") MultipartFile archivo) {

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío");
        }
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("El archivo debe tener extensión .csv");
        }
        if (archivo.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("El archivo supera el tamaño máximo permitido (10MB)");
        }

        List<FilaImportacion> validas = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String primeraLinea = reader.readLine();
            // Detectamos el separador real del archivo: puede ser ';' (nuestro formato,
            // pensado para Excel en español) o ',' (formato estandar/ingles, comun si el
            // archivo se reguardo desde una planilla de calculo con otra config regional)
            char separador = primeraLinea.contains(";") ? ';' : ',';
            if (primeraLinea == null) {
                return ResponseEntity.ok(new ImportacionPreviewResponse(0, validas,
                        List.of("El archivo está vacío o no tiene encabezado")));
            }
            // Sacamos un posible BOM UTF-8 al principio del archivo (nuestro propio export lo incluye)
            if (primeraLinea.startsWith("\uFEFF")) {
                primeraLinea = primeraLinea.substring(1);
            }

            // Mapeamos el nombre de cada columna del encabezado a su posicion,
            // para no depender de un orden fijo (funciona tanto con el CSV que exportamos
            // nosotros mismos -que incluye ID y TieneComprobante- como con uno armado a mano)
            String[] encabezados = primeraLinea.split(String.valueOf(separador), -1);
            java.util.Map<String, Integer> colIndex = new java.util.HashMap<>();
            for (int i = 0; i < encabezados.length; i++) {
                colIndex.put(encabezados[i].trim().toLowerCase(), i);
            }

            Integer idxTipo = colIndex.get("tipo");
            Integer idxMonto = colIndex.get("monto");
            Integer idxFecha = colIndex.get("fecha");
            Integer idxCategoria = colIndex.get("categoria");
            Integer idxDescripcion = colIndex.get("descripcion");

            if (idxTipo == null || idxMonto == null || idxFecha == null || idxCategoria == null) {
                return ResponseEntity.badRequest().body(
                        "El archivo debe tener las columnas: Tipo, Monto, Fecha y Categoria (separadas por ';'). " +
                                "Descripcion es opcional."
                );
            }

            String linea;
            int numeroFila = 1;

            while ((linea = reader.readLine()) != null) {
                numeroFila++;
                if (linea.isBlank()) continue;

                String[] campos = linea.split(String.valueOf(separador), -1);
                int maxIdx = Math.max(Math.max(idxTipo, idxMonto), Math.max(idxFecha, idxCategoria));
                if (campos.length <= maxIdx) {
                    errores.add("Fila " + numeroFila + ": faltan campos obligatorios");
                    continue;
                }

                try {
                    String tipo = campos[idxTipo].trim().toUpperCase();
                    if (!tipo.equals("INGRESO") && !tipo.equals("EGRESO")) {
                        errores.add("Fila " + numeroFila + ": tipo inválido (debe ser INGRESO o EGRESO)");
                        continue;
                    }
                    BigDecimal monto = new BigDecimal(campos[idxMonto].trim());
                    if (monto.signum() <= 0) {
                        errores.add("Fila " + numeroFila + ": el monto debe ser positivo");
                        continue;
                    }
                    LocalDate fecha = parsearFecha(campos[idxFecha].trim());
                    String categoriaNombre = campos[idxCategoria].trim();
                    if (categoriaNombre.isEmpty()) {
                        errores.add("Fila " + numeroFila + ": la categoría es obligatoria");
                        continue;
                    }
                    String descripcion = (idxDescripcion != null && campos.length > idxDescripcion)
                            ? campos[idxDescripcion].trim() : "";

                    validas.add(new FilaImportacion(tipo, monto, fecha, categoriaNombre, descripcion));
                } catch (Exception e) {
                    errores.add("Fila " + numeroFila + ": formato inválido (" + e.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error al leer el archivo");
        }

        return ResponseEntity.ok(new ImportacionPreviewResponse(validas.size(), validas, errores));
    }

    // CU-071 (paso 2): Confirmar la importacion de las filas validadas en el preview
    @PostMapping("/importar/csv/confirmar")
    public ResponseEntity<?> confirmarImportacion(@RequestBody List<FilaImportacion> filas, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        int importadas = 0;

        for (FilaImportacion fila : filas) {
            Categoria categoria = categoriaRepository
                    .findByPredefinidaTrueOrUsuarioId(usuario.getId()).stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(fila.getCategoriaNombre()))
                    .findFirst().orElse(null);

            if (categoria == null) continue; // categoria no encontrada: se omite esa fila

            Transaccion t = new Transaccion();
            t.setUsuario(usuario);
            t.setTipo(TipoTransaccion.valueOf(fila.getTipo()));
            t.setMonto(fila.getMonto());
            t.setFecha(fila.getFecha());
            t.setCategoria(categoria);
            t.setDescripcion(fila.getDescripcion());
            t.setEsRecurrente(false);
            transaccionRepository.save(t);
            importadas++;
        }

        return ResponseEntity.ok("Se importaron " + importadas + " transacción(es) correctamente");
    }

    // CU-072: Exportar respaldo completo de datos
    @GetMapping("/respaldo")
    public ResponseEntity<byte[]> exportarRespaldo(Authentication authentication) throws Exception {
        Usuario usuario = usuarioActual(authentication);

        List<Categoria> propias = categoriaRepository.findByPredefinidaTrueOrUsuarioId(usuario.getId())
                .stream().filter(c -> !c.isPredefinida()).toList();
        List<CategoriaRespaldo> categoriasRespaldo = propias.stream()
                .map(c -> new CategoriaRespaldo(c.getNombre(), c.getTipo().name(), c.getColor()))
                .toList();

        List<Transaccion> transacciones = transaccionRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId());
        List<TransaccionRespaldo> transaccionesRespaldo = transacciones.stream()
                .map(t -> new TransaccionRespaldo(t.getTipo().name(), t.getMonto(), t.getFecha(),
                        t.getCategoria().getNombre(), t.getDescripcion()))
                .toList();

        RespaldoCompleto respaldo = new RespaldoCompleto(
                "1.0", LocalDateTime.now(), usuario.getEmail(), usuario.getModulosActivos(),
                categoriasRespaldo, transaccionesRespaldo
        );

        byte[] contenido = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(respaldo);
        String fechaArchivo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = "FinNova_Respaldo_" + fechaArchivo + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(contenido);
    }

    // CU-073 (paso 1): Vista previa de la restauracion, valida sin aplicar nada
    @PostMapping(value = "/importar/respaldo/preview", consumes = "multipart/form-data")
    public ResponseEntity<?> previsualizarRestauracion(
            @org.springframework.web.bind.annotation.RequestParam("archivo") MultipartFile archivo) {

        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.toLowerCase().endsWith(".json")) {
            return ResponseEntity.badRequest().body("El archivo debe tener extensión .json");
        }

        RespaldoCompleto respaldo;
        try {
            respaldo = objectMapper.readValue(archivo.getInputStream(), RespaldoCompleto.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("El archivo no tiene un formato de respaldo válido de FinNova");
        }

        if (respaldo.getVersion() == null || !respaldo.getVersion().startsWith("1.")) {
            return ResponseEntity.badRequest().body("La versión del respaldo no es compatible con esta versión del sistema");
        }
        if (respaldo.getCategoriasPersonalizadas() == null || respaldo.getTransacciones() == null) {
            return ResponseEntity.badRequest().body("El archivo de respaldo está incompleto o corrupto");
        }

        return ResponseEntity.ok(new RestauracionPreviewResponse(
                respaldo.getFechaGeneracion().toString(),
                respaldo.getCategoriasPersonalizadas().size(),
                respaldo.getTransacciones().size(),
                respaldo
        ));
    }

    // CU-073 (paso 2): Confirmar la restauracion, reemplaza los datos actuales del usuario
    @PostMapping("/importar/respaldo/confirmar")
    public ResponseEntity<?> confirmarRestauracion(@RequestBody RespaldoCompleto respaldo, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);

        // 1. Borrar transacciones actuales del usuario (deben ir antes que las categorias, por la FK)
        List<Transaccion> actuales = transaccionRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId());
        transaccionRepository.deleteAll(actuales);

        // 2. Borrar categorias personalizadas actuales del usuario
        List<Categoria> categoriasActuales = categoriaRepository.findByPredefinidaTrueOrUsuarioId(usuario.getId())
                .stream().filter(c -> !c.isPredefinida()).toList();
        categoriaRepository.deleteAll(categoriasActuales);

        // 3. Recrear las categorias personalizadas del respaldo
        for (CategoriaRespaldo cr : respaldo.getCategoriasPersonalizadas()) {
            Categoria nueva = new Categoria();
            nueva.setNombre(cr.getNombre());
            nueva.setTipo(TipoCategoria.valueOf(cr.getTipo()));
            nueva.setColor(cr.getColor());
            nueva.setPredefinida(false);
            nueva.setUsuario(usuario);
            categoriaRepository.save(nueva);
        }

        // 4. Recrear las transacciones del respaldo (buscando la categoria por nombre,
        //    entre las predefinidas + las recien recreadas)
        List<Categoria> categoriasDisponibles = categoriaRepository.findByPredefinidaTrueOrUsuarioId(usuario.getId());
        int restauradas = 0;
        for (TransaccionRespaldo tr : respaldo.getTransacciones()) {
            Categoria categoria = categoriasDisponibles.stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(tr.getCategoriaNombre()))
                    .findFirst().orElse(null);
            if (categoria == null) continue;

            Transaccion t = new Transaccion();
            t.setUsuario(usuario);
            t.setTipo(TipoTransaccion.valueOf(tr.getTipo()));
            t.setMonto(tr.getMonto());
            t.setFecha(tr.getFecha());
            t.setCategoria(categoria);
            t.setDescripcion(tr.getDescripcion());
            t.setEsRecurrente(false);
            transaccionRepository.save(t);
            restauradas++;
        }

        // 5. Restaurar la configuracion de modulos activos
        if (respaldo.getModulosActivos() != null) {
            usuario.setModulosActivos(respaldo.getModulosActivos());
            usuarioRepository.save(usuario);
        }

        return ResponseEntity.ok("Restauración completada: " + respaldo.getCategoriasPersonalizadas().size() +
                " categoría(s) y " + restauradas + " transacción(es) restauradas.");
    }
}