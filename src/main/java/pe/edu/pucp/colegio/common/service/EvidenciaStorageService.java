package pe.edu.pucp.colegio.common.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.exception.BusinessException;

@Service
public class EvidenciaStorageService {
  private static final long MAX_BYTES = 10L * 1024 * 1024;
  private final Path raiz;

  public EvidenciaStorageService(@Value("${app.storage.evidence-dir:uploads/evidencias}") String directorio) {
    this.raiz = Path.of(directorio).toAbsolutePath().normalize();
  }

  public ArchivoGuardado guardar(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) throw new BusinessException("Debe adjuntar un archivo no vacío");
    if (archivo.getSize() > MAX_BYTES) throw new BusinessException("El archivo supera el máximo de 10 MB");
    String nombre = Path.of(archivo.getOriginalFilename() == null ? "evidencia" : archivo.getOriginalFilename()).getFileName().toString();
    String extension = "";
    int punto = nombre.lastIndexOf('.');
    if (punto >= 0) extension = nombre.substring(punto).replaceAll("[^A-Za-z0-9.]", "");
    String id = UUID.randomUUID() + extension;
    try {
      Files.createDirectories(raiz);
      Files.copy(archivo.getInputStream(), raiz.resolve(id), StandardCopyOption.REPLACE_EXISTING);
      return new ArchivoGuardado(id, nombre, archivo.getContentType(), archivo.getSize());
    } catch (IOException e) {
      throw new BusinessException("No se pudo guardar la evidencia adjunta");
    }
  }

  public Resource cargar(String clave) {
    Path archivo = raiz.resolve(clave).normalize();
    if (!archivo.startsWith(raiz)) throw new BusinessException("Evidencia no encontrada");
    try {
      Resource recurso = new UrlResource(archivo.toUri());
      if (!recurso.exists() || !recurso.isReadable()) throw new BusinessException("Evidencia no encontrada");
      return recurso;
    } catch (IOException e) {
      throw new BusinessException("Evidencia no encontrada");
    }
  }

  public record ArchivoGuardado(String clave, String nombre, String tipo, long tamanio) {}
}
