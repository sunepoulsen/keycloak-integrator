package dk.sunepoulsen.keycloak.integrator.model;

import dk.sunepoulsen.tes.springboot.client.core.rs.model.BaseModel;
import lombok.Data;

import java.util.Map;

@Data
public class AccessTokenModel implements BaseModel {
    private long expiresIn;
    private String idToken;
    private int notBeforePolicy;
    private Map<String, Object> otherClaims;
    private long refreshExpiresIn;
    private String refreshToken;
    private String scope;
    private String sessionState;
    private String token;
    private String tokenType;
}
