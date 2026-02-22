package ec.edu.uteq.microservicios.msusuarios.mapper;

import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaCreateRequest;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaDto;
import ec.edu.uteq.microservicios.msusuarios.api.model.EntregaUpdateRequest;
import ec.edu.uteq.microservicios.msusuarios.model.Entrega;
import org.openapitools.jackson.nullable.JsonNullable;

public class EntregaMapper {

    private EntregaMapper() {}

    private static JsonNullable<String> toJsonNullable(String value) {
        return value == null ? JsonNullable.undefined() : JsonNullable.of(value);
    }

    private static String fromJsonNullable(JsonNullable<String> value) {
        if (value == null || !value.isPresent()) return null;
        return value.get();
    }

    public static EntregaDto toDto(Entrega e) {
        if (e == null) return null;

        EntregaDto dto = new EntregaDto();
        dto.setId(e.getId());
        dto.setOrderId(e.getOrderId());
        dto.setAddress(e.getAddress());

        dto.setTrackingNumber(toJsonNullable(e.getTrackingNumber()));
        dto.setEmail(toJsonNullable(e.getEmail()));

        if (e.getStatus() != null) {
            // Convertimos el Enum de la Entidad al Enum del DTO
            dto.setStatus(EntregaDto.StatusEnum.fromValue(e.getStatus().name()));
        }

        return dto;
    }

    public static Entrega toEntity(EntregaCreateRequest req) {
        if (req == null) return null;

        Entrega e = new Entrega();
        e.setOrderId(req.getOrderId());
        e.setAddress(req.getAddress());

        e.setTrackingNumber(fromJsonNullable(req.getTrackingNumber()));
        e.setEmail(fromJsonNullable(req.getEmail()));

        if (req.getStatus() != null) {
            e.setStatus(Entrega.Estado.valueOf(req.getStatus().getValue()));
        }

        return e;
    }

    public static Entrega toEntity(EntregaUpdateRequest req) {
        if (req == null) return null;

        Entrega e = new Entrega();
        e.setOrderId(req.getOrderId());
        e.setAddress(req.getAddress());

        e.setTrackingNumber(fromJsonNullable(req.getTrackingNumber()));
        e.setEmail(fromJsonNullable(req.getEmail()));

        if (req.getStatus() != null) {
            e.setStatus(Entrega.Estado.valueOf(req.getStatus().getValue()));
        }

        return e;
    }
}