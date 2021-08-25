package dk.sunepoulsen.keycloak.integrator.model;

import dk.sunepoulsen.tes.springboot.client.core.rs.model.BaseModel;
import lombok.Data;

@Data
public class KeycloakError implements BaseModel {
    private String error;
    private String errorDescription;
}
