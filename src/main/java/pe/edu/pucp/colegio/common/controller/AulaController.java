package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AulaRequest;
import pe.edu.pucp.colegio.common.dto.AulaResponse;
import pe.edu.pucp.colegio.common.dto.CambioEstadoAulaRequest;
import pe.edu.pucp.colegio.common.service.AulaService;

/** Expone las operaciones de administración de aulas. */
@RestController
@RequestMapping("/api/v1/aulas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AulaController {

    private final AulaService aulas;

    public AulaController(AulaService aulas) {
        this.aulas = aulas;
    }

    @GetMapping
    public List<AulaResponse> listar() {
        return aulas.listar();
    }

    @GetMapping("/{id}")
    public AulaResponse obtener(@PathVariable Integer id) {
        return aulas.obtener(id);
    }

    @PostMapping
    public ResponseEntity<AulaResponse> crear(@Valid @RequestBody AulaRequest request) {
        AulaResponse creada = aulas.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/aulas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public AulaResponse actualizar(
            @PathVariable Integer id, @Valid @RequestBody AulaRequest request) {
        return aulas.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public AulaResponse cambiarEstado(
            @PathVariable Integer id, @Valid @RequestBody CambioEstadoAulaRequest request) {
        return aulas.cambiarEstado(id, request);
    }
}
