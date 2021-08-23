package dk.sunepoulsen.keycloak.integrator;

import dk.sunepoulsen.keycloak.integrator.model.AccessTokenModel;
import io.reactivex.Single;

public class KeycloakIntegrator {
    public Single<AccessTokenModel> login(String realm, String username, String passwd) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}
