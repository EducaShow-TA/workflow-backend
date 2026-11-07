package pe.edu.pucp.colegio.security.service.impl;

import pe.edu.pucp.colegio.security.model.Rol;

public record ResultadoProcesamientoFila(
    Rol rol,
    String dni,
    String username,
    boolean cuentaCreada,
    boolean datosActualizados,
    boolean vinculoCreado,
    String contrasenhaTemporal) {}
