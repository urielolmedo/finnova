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
                    new Categoria(null, "Sueldo", TipoCategoria.INGRESO, true, null),
                    new Categoria(null, "Ventas", TipoCategoria.INGRESO, true, null),
                    new Categoria(null, "Otros ingresos", TipoCategoria.INGRESO, true, null),
                    new Categoria(null, "Alimentación", TipoCategoria.EGRESO, true, null),
                    new Categoria(null, "Transporte", TipoCategoria.EGRESO, true, null),
                    new Categoria(null, "Servicios", TipoCategoria.EGRESO, true, null),
                    new Categoria(null, "Alquiler", TipoCategoria.EGRESO, true, null),
                    new Categoria(null, "Entretenimiento", TipoCategoria.EGRESO, true, null),
                    new Categoria(null, "Salud", TipoCategoria.EGRESO, true, null),
                    new Categoria(null, "Otros gastos", TipoCategoria.EGRESO, true, null)
            );
            categoriaRepository.saveAll(predefinidas);
            System.out.println(">> DataSeeder: " + predefinidas.size() + " categorias predefinidas cargadas.");
        }
    }
}