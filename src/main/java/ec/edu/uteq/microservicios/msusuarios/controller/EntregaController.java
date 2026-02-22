package ec.edu.uteq.microservicios.msusuarios.controller;

import ec.edu.uteq.microservicios.msusuarios.api.EntregasApi;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaCreateRequest;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaDto;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaUpdateRequest;
import ec.edu.uteq.microservicios.msusuarios.mapper.EntregaMapper;
import ec.edu.uteq.microservicios.msusuarios.model.Entrega;
import ec.edu.uteq.microservicios.msusuarios.service.EntregaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class EntregaController implements EntregasApi {

    private final EntregaService service;

    public EntregaController(EntregaService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<EntregaDto>> listarEntregas() {
        List<EntregaDto> dtos = service.listar()
                .stream()
                .map(EntregaMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<EntregaDto> crearEntrega(EntregaCreateRequest entregaCreateRequest) {
        Entrega entity = EntregaMapper.toEntity(entregaCreateRequest);
        Entrega created = service.crear(entity);

        EntregaDto dto = EntregaMapper.toDto(created);

        return ResponseEntity
                .created(URI.create("/api/entregas/" + created.getId()))
                .body(dto);
    }

    @Override
    public ResponseEntity<EntregaDto> actualizarEntrega(Long id, EntregaUpdateRequest entregaUpdateRequest) {
        Entrega entity = EntregaMapper.toEntity(entregaUpdateRequest);
        Entrega updated = service.actualizar(id, entity);

        return ResponseEntity.ok(EntregaMapper.toDto(updated));
    }

    @DeleteMapping("/api/entregas/{id}")
    public ResponseEntity<Void> eliminarEntrega(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/entregas/search/{trackingNumber}")
    public ResponseEntity<EntregaDto> buscarPorTracking(@PathVariable String trackingNumber) {
        return service.buscarPorTracking(trackingNumber)
                .map(EntregaMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/entregas/estadisticas")
    public ResponseEntity<Map<String, Long>> obtenerEstadisticas() {
        return ResponseEntity.ok(service.obtenerEstadisticas());
    }
}
