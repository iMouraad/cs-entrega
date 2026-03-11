package ec.edu.uteq.microservicios.msusuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClienteDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String cedula;
    private String telefono;
    private String direccion;
}
