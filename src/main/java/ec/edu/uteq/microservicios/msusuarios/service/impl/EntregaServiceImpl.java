package ec.edu.uteq.microservicios.msusuarios.service.impl;

import ec.edu.uteq.microservicios.msusuarios.model.ClienteDTO;
import ec.edu.uteq.microservicios.msusuarios.model.Entrega;
import ec.edu.uteq.microservicios.msusuarios.model.FacturaDTO;
import ec.edu.uteq.microservicios.msusuarios.repository.EntregaRepository;
import ec.edu.uteq.microservicios.msusuarios.service.EntregaService;
import ec.edu.uteq.microservicios.msusuarios.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.*;

@Slf4j
@Service
public class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository repo;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    public EntregaServiceImpl(EntregaRepository repo, EmailService emailService, RestTemplate restTemplate) {
        this.repo = repo;
        this.emailService = emailService;
        this.restTemplate = restTemplate;
    }

    @Value("${api.facturacion.url}")
    private String facturacionApiUrl;

    @Value("${api.clientes.url}")
    private String clientesApiUrl;

    @Value("${api.simulation.enabled:false}")
    private boolean simulationEnabled;

    @Override
    @CircuitBreaker(name = "facturacionCB", fallbackMethod = "fallbackObtenerFacturas")
    public List<FacturaDTO> obtenerFacturasExternas() {
        if (simulationEnabled) {
            return obtenerFacturasDePrueba();
        }
        FacturaDTO[] facturas = restTemplate.getForObject(facturacionApiUrl, FacturaDTO[].class);
        return facturas != null ? Arrays.asList(facturas) : Collections.emptyList();
    }

    public List<FacturaDTO> fallbackObtenerFacturas(Throwable t) {
        log.warn("Circuit Breaker activado para Facturas. Razón: {}. Retornando simulación.", t.getMessage());
        return obtenerFacturasDePrueba();
    }

    @Override
    @CircuitBreaker(name = "clientesCB", fallbackMethod = "fallbackObtenerClientes")
    public List<ClienteDTO> obtenerClientesExternos() {
        if (simulationEnabled) {
            return obtenerClientesDePrueba();
        }
        ClienteDTO[] clientes = restTemplate.getForObject(clientesApiUrl, ClienteDTO[].class);
        return clientes != null ? Arrays.asList(clientes) : Collections.emptyList();
    }

    public List<ClienteDTO> fallbackObtenerClientes(Throwable t) {
        log.warn("Circuit Breaker activado para Clientes. Razón: {}. Retornando simulación.", t.getMessage());
        return obtenerClientesDePrueba();
    }

    private List<FacturaDTO> obtenerFacturasDePrueba() {
        FacturaDTO f1 = new FacturaDTO();
        f1.setId(101L); f1.setCliente("Juan Pérez (SIMULADO)"); f1.setTotal(150.50); f1.setFecha("2024-03-10"); f1.setEstado("PAGADO");
        
        FacturaDTO f2 = new FacturaDTO();
        f2.setId(102L); f2.setCliente("Maria Garcia (SIMULADO)"); f2.setTotal(89.99); f2.setFecha("2024-03-11"); f2.setEstado("PENDIENTE");
        
        return Arrays.asList(f1, f2);
    }

    private List<ClienteDTO> obtenerClientesDePrueba() {
        ClienteDTO c1 = new ClienteDTO();
        c1.setId(1L); c1.setNombre("Carlos"); c1.setApellido("Soto"); c1.setCedula("1234567890"); c1.setEmail("carlos@ejemplo.com");
        
        ClienteDTO c2 = new ClienteDTO();
        c2.setId(2L); c2.setNombre("Ana"); c2.setApellido("Velez"); c2.setCedula("0987654321"); c2.setEmail("ana@ejemplo.com");
        
        return Arrays.asList(c1, c2);
    }

    @Override
    public Entrega prepararEntregaPorOrden(Long orderId) {
        log.info("Preparando datos para la Orden #{}", orderId);
        
        // 1. Buscar la Factura/Orden
        List<FacturaDTO> facturas = obtenerFacturasExternas();
        FacturaDTO factura = facturas.stream()
                .filter(f -> f.getId().equals(orderId))
                .findFirst()
                .orElse(null);

        Entrega entregaPrevia = new Entrega();
        entregaPrevia.setOrderId(orderId);
        entregaPrevia.setStatus(Entrega.Estado.PENDIENTE);

        if (factura != null) {
            entregaPrevia.setCustomerName(factura.getCliente());
            // 2. Intentar buscar al cliente para obtener dirección y email real
            List<ClienteDTO> clientes = obtenerClientesExternos();
            // Buscamos por nombre o podríamos buscar por ID si la factura lo tuviera
            ClienteDTO cliente = clientes.stream()
                    .filter(c -> factura.getCliente().contains(c.getNombre()))
                    .findFirst()
                    .orElse(null);

            if (cliente != null) {
                entregaPrevia.setAddress(cliente.getDireccion());
                entregaPrevia.setEmail(cliente.getEmail());
            } else {
                // Datos por defecto si no hay match de cliente
                entregaPrevia.setAddress("Dirección pendiente de confirmar");
                entregaPrevia.setEmail("cliente@ejemplo.com");
            }
        } else {
            // Si no existe la orden en facturación
            entregaPrevia.setAddress("Orden no encontrada en Facturación");
            entregaPrevia.setStatus(Entrega.Estado.CANCELADO);
        }

        return entregaPrevia;
    }

    @Override
    public List<Entrega> listar() {
        return repo.findAll();
    }

    @Override
    public Entrega crear(Entrega entrega) {
        if (entrega == null) throw new RuntimeException("Datos nulos");

        // VALIDACIÓN: Evitar ID de Orden duplicado
        if (repo.existsByOrderId(entrega.getOrderId())) {
            throw new RuntimeException("Error: Ya existe una entrega con la Orden #" + entrega.getOrderId());
        }

        if (entrega.getStatus() == null) {
            entrega.setStatus(Entrega.Estado.PENDIENTE);
        }

        Entrega guardada = repo.save(entrega);

        String cuerpo = construirMensaje(guardada, "¡Tu pedido ha sido registrado!",
                "Estamos preparando todo para procesar tu entrega lo antes posible.");

        enviarNotificacion(guardada, "Confirmación de Pedido #" + guardada.getOrderId(), cuerpo);

        return guardada;
    }

    @Override
    public Entrega actualizar(Long id, Entrega entrega) {
        return repo.findById(id).map(existente -> {

            Optional<Entrega> otraConMismoId = repo.findByOrderId(entrega.getOrderId());
            if (otraConMismoId.isPresent() && !otraConMismoId.get().getId().equals(id)) {
                throw new RuntimeException("Error: El ID de Orden #" + entrega.getOrderId() + " ya está en uso.");
            }

            validarCambioEstado(existente.getStatus(), entrega.getStatus());

            existente.setOrderId(entrega.getOrderId());
            existente.setAddress(entrega.getAddress());
            existente.setCustomerName(entrega.getCustomerName());

            boolean cambioEstado = existente.getStatus() != entrega.getStatus();
            existente.setStatus(entrega.getStatus());
            existente.setTrackingNumber(entrega.getTrackingNumber());
            existente.setEmail(entrega.getEmail());

            Entrega actualizada = repo.save(existente);

            if (cambioEstado) {
                String titulo = "";
                String nota = "";

                switch (actualizada.getStatus()) {
                    case ENVIADO:
                        titulo = "¡Tu paquete está en camino! 🚚";
                        nota = "El repartidor ya tiene tu pedido y se dirige a la dirección registrada.";
                        break;
                    case ENTREGADO:
                        titulo = "¡Pedido entregado con éxito! ✅";
                        nota = "Esperamos que disfrutes tu compra. Gracias por confiar en nosotros.";
                        break;
                    case CANCELADO:
                        titulo = "Tu orden ha sido cancelada ❌";
                        nota = "Te informamos que el envío ha sido anulado. Si tienes dudas, contáctanos.";
                        break;
                    default:
                        titulo = "Actualización de tu pedido";
                        nota = "Hay novedades en el proceso de tu entrega.";
                }

                String cuerpo = construirMensaje(actualizada, titulo, nota);
                enviarNotificacion(actualizada, "Novedades en tu Orden #" + actualizada.getOrderId(), cuerpo);
            }
            return actualizada;
        }).orElseThrow(() -> new RuntimeException("No encontrado"));
    }


    private String construirMensaje(Entrega e, String saludo, String notaEstado) {
        return saludo + "\n\n" +
                "Detalles del envío:\n" +
                "--------------------------------------\n" +
                "📦 Orden: #" + e.getOrderId() + "\n" +
                "📍 Estado: " + e.getStatus() + "\n" +
                "🏠 Dirección: " + e.getAddress() + "\n" +
                "🔢 Seguimiento: " + (e.getTrackingNumber() != null ? e.getTrackingNumber() : "No asignado") + "\n" +
                "--------------------------------------\n\n" +
                notaEstado + "\n\n" +
                "Atentamente,\n" +
                "Sistema de Gestión de Entregas.";
    }

    @Override
    public void eliminar(Long id) { repo.deleteById(id); }

    @Override
    public Optional<Entrega> buscarPorTracking(String trackingNumber) { return repo.findByTrackingNumber(trackingNumber); }

    @Override
    public Map<String, Long> obtenerEstadisticas() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDIENTE", repo.countByStatus(Entrega.Estado.PENDIENTE));
        stats.put("ENVIADO", repo.countByStatus(Entrega.Estado.ENVIADO));
        stats.put("ENTREGADO", repo.countByStatus(Entrega.Estado.ENTREGADO));
        stats.put("CANCELADO", repo.countByStatus(Entrega.Estado.CANCELADO));
        return stats;
    }

    private void validarCambioEstado(Entrega.Estado actual, Entrega.Estado nuevo) {
        if (actual == nuevo) return;
        boolean esValido = switch (actual) {
            case PENDIENTE -> nuevo == Entrega.Estado.ENVIADO || nuevo == Entrega.Estado.CANCELADO;
            case ENVIADO   -> nuevo == Entrega.Estado.ENTREGADO || nuevo == Entrega.Estado.CANCELADO;
            default -> false;
        };
        if (!esValido) throw new RuntimeException("Transición de estado no permitida");
    }

    @Override
    public Optional<Entrega> buscarPorId(Long id) {
        return repo.findById(id);
    }

    private void enviarNotificacion(Entrega entrega, String asunto, String cuerpo) {
        if (entrega.getEmail() != null && !entrega.getEmail().isEmpty()) {
            emailService.enviarNotificacion(entrega.getEmail(), asunto, cuerpo);
        }
    }
}