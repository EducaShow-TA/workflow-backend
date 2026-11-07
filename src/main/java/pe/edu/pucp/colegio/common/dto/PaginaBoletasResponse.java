package pe.edu.pucp.colegio.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Contrato estable de paginación: number empieza en cero. */
public record PaginaBoletasResponse(
    List<BoletaPensionResponse> content, long totalElements, int totalPages, int number, int size) {
  public static PaginaBoletasResponse desde(Page<BoletaPensionResponse> pagina) {
    return new PaginaBoletasResponse(
        pagina.getContent(),
        pagina.getTotalElements(),
        pagina.getTotalPages(),
        pagina.getNumber(),
        pagina.getSize());
  }
}
