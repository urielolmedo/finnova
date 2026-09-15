package ar.edu.usal.finnova.config;

import ar.edu.usal.finnova.model.Categoria;
import ar.edu.usal.finnova.model.TipoCategoria;
import ar.edu.usal.finnova.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() == 0) {
            List<Categoria> predefinidas = List.of(
                    new Categoria(null, "Sueldo", TipoCategoria.INGRESO, true, null, "#2e7d32"),
                    new Categoria(null, "Ventas", TipoCategoria.INGRESO, true, null, "#388e3c"),
                    new Categoria(null, "Otros ingresos", TipoCategoria.INGRESO, true, null, "#43a047"),
                    new Categoria(null, "Alimentación", TipoCategoria.EGRESO, true, null, "#e65100"),
                    new Categoria(null, "Transporte", TipoCategoria.EGRESO, true, null, "#1565c0"),
                    new Categoria(null, "Servicios", TipoCategoria.EGRESO, true, null, "#6a1b9a"),
                    new Categoria(null, "Alquiler", TipoCategoria.EGRESO, true, null, "#ad1457"),
                    new Categoria(null, "Entretenimiento", TipoCategoria.EGRESO, true, null, "#00838f"),
                    new Categoria(null, "Salud", TipoCategoria.EGRESO, true, null, "#c62828"),
                    new Categoria(null, "Otros gastos", TipoCategoria.EGRESO, true, null, "#6c757d")
            );
            categoriaRepository.saveAll(predefinidas);
            System.out.println(">> DataSeeder: " + predefinidas.size() + " categorias predefinidas cargadas.");
        }
    }
}