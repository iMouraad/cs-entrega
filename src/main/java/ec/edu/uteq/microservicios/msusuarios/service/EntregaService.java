package ec.edu.uteq.microservicios.msusuarios.service;

import ec.edu.uteq.microservicios.msusuarios.model.Entrega;
//import org.springframework.lang.NonNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EntregaService {
    List<Entrega> listar();
    Entrega crear(Entrega entrega);
    Entrega actualizar(Long id, Entrega entrega);
    void eliminar(Long id);
    Optional<Entrega> buscarPorTracking(String trackingNumber);
    Map<String, Long> obtenerEstadisticas();
}