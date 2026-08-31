package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.CategoriaResponse;
import ar.edu.usal.finnova.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    @GetMapping
    public List<CategoriaResponse> listar(Authentication authentication) {
        // Por ahora solo trae predefinidas (no hay categorias personalizadas hasta el Sprint 3)
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponse::new)
                .toList();
    }
}