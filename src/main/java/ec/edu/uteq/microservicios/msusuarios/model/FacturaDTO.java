package ec.edu.uteq.microservicios.msusuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacturaDTO {
    private Long id;
    private String cliente;
    private Double total;
    private String fecha;
    private String estado;
}