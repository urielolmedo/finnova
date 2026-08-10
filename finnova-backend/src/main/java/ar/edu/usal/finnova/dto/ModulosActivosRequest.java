package ar.edu.usal.finnova.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ModulosActivosRequest {
    private List<String> modulos;
}
