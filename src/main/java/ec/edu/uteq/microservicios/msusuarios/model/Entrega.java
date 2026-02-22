package ec.edu.uteq.microservicios.msusuarios.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entregas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Entrega {

    public enum Estado {
        PENDIENTE,
        ENVIADO,
        ENTREGADO,
        CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Estado status;

    private String trackingNumber;

    @Column(length = 100)
    private String email;
}