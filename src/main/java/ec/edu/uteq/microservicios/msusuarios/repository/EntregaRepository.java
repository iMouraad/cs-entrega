package ec.edu.uteq.microservicios.msusuarios.repository;

import ec.edu.uteq.microservicios.msusuarios.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    Optional<Entrega> findByTrackingNumber(String trackingNumber);
    long countByStatus(Entrega.Estado status);

    boolean existsByOrderId(Long orderId);
    Optional<Entrega> findByOrderId(Long orderId);
}