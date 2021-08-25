package dk.sunepoulsen.keycloak.integrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sunepoulsen.keycloak.integrator.model.KeycloakError;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientBadRequestException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientConflictException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientInternalServerException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientNotFoundException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientNotImplementedException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientResponseException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientUnauthorizedException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.DecodeJsonException;
import dk.sunepoulsen.tes.springboot.client.core.rs.model.ServiceError;
import dk.sunepoulsen.tes.springboot.client.core.rs.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpResponse;

@Slf4j
public class ResponseHandler {

    private final JsonMapper jsonMapper;

    public ResponseHandler() {
        this(new JsonMapper());
    }

    public ResponseHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String verifyResponseAndExtractBody(HttpResponse<String> response) {
        if( response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }

        switch(response.statusCode()) {
            case 400:
                throw new ClientBadRequestException(response, decodeErrorBody(response.body()));

            case 401:
                throw new ClientUnauthorizedException(response, decodeErrorBody(response.body()));

            case 404:
                throw new ClientNotFoundException(response, decodeErrorBody(response.body()));

            case 409:
                throw new ClientConflictException(response, decodeErrorBody(response.body()));

            case 500:
                throw new ClientInternalServerException(response, decodeErrorBody(response.body()));

            case 501:
                throw new ClientNotImplementedException(response, decodeErrorBody(response.body()));

            default:
                throw new ClientResponseException(response, decodeErrorBody(response.body()));
        }
    }

    private ServiceError decodeErrorBody(String body) {
        KeycloakError keycloakError = jsonMapper.decode(body, KeycloakError.class);

        ServiceError serviceError = new ServiceError();
        serviceError.setCode(keycloakError.getError());
        serviceError.setMessage(keycloakError.getErrorDescription());

        return serviceError;
    }

}
