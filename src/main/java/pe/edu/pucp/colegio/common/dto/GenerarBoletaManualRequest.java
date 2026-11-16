package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;

public record GenerarBoletaManualRequest(
    @NotNull Integer matriculaId, @NotNull YearMonth mesCobro) {}
