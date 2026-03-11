package ec.edu.uteq.microservicios.msusuarios.controller;

import ec.edu.uteq.microservicios.msusuarios.api.EntregasApi;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaCreateRequest;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaDto;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaUpdateRequest;
import ec.edu.uteq.microservicios.msusuarios.mapper.EntregaMapper;
import ec.edu.uteq.microservicios.msusuarios.model.ClienteDTO;
import ec.edu.uteq.microservicios.msusuarios.model.Entrega;
import ec.edu.uteq.microservicios.msusuarios.model.FacturaDTO;
import ec.edu.uteq.microservicios.msusuarios.service.EntregaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ec.edu.uteq.microservicios.msusuarios.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class EntregaController implements EntregasApi {

    private final EntregaService service;
    private final PdfService pdfService;

    public EntregaController(EntregaService service, PdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping("/api/entregas/facturas-externas")
    public ResponseEntity<List<FacturaDTO>> listarFacturasExternas() {
        return ResponseEntity.ok(service.obtenerFacturasExternas());
    }

    @GetMapping("/api/entregas/clientes-externos")
    public ResponseEntity<List<ClienteDTO>> listarClientesExternos() {
        return ResponseEntity.ok(service.obtenerClientesExternos());
    }

    @GetMapping("/api/entregas/preparar/{orderId}")
    public ResponseEntity<EntregaDto> prepararEntrega(@PathVariable Long orderId) {
        Entrega preEntrega = service.prepararEntregaPorOrden(orderId);
        return ResponseEntity.ok(EntregaMapper.toDto(preEntrega));
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

    @GetMapping("/api/entregas/{id}/pdf")
    public ResponseEntity<byte[]> descargarGuiaPdf(@PathVariable Long id) {
        Entrega entrega = service.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Entrega no encontrada"));
        
        byte[] pdf = pdfService.generateDeliveryNote(entrega);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guia_entrega_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
