package ec.edu.uteq.microservicios.msusuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacturaDetalleDTO {
    private Long id;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    private Long productoId;
    private String productoNombre;
}
