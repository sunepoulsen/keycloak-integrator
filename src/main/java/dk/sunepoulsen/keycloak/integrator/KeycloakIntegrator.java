package dk.sunepoulsen.keycloak.integrator;

import dk.sunepoulsen.keycloak.integrator.model.AccessTokenModel;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientException;
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ServiceNotAvailableException;
import dk.sunepoulsen.tes.springboot.client.core.rs.integrations.config.DefaultClientConfig;
import dk.sunepoulsen.tes.springboot.client.core.rs.integrations.config.TechEasySolutionsClientConfig;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class KeycloakIntegrator {

    static final Long DEFAULT_TIMEOUT = 15000L;
    static final Long DEFAULT_SLEEP = 300L;

    private final URI uri;
    private final String realm;
    private final String clientId;
    private final TechEasySolutionsClientConfig config;
    private final HttpClient client;

    private final ResponseHandler responseHandler;

    public KeycloakIntegrator(URI uri, String realm, String clientId) {
        this(uri, realm, clientId, new DefaultClientConfig());
    }

    public KeycloakIntegrator(URI uri, String realm, String clientId, TechEasySolutionsClientConfig config) {
        this.uri = uri;
        this.realm = realm;
        this.clientId = clientId;
        this.config = config;

        this.client = buildHttpClient();
        this.responseHandler = new ResponseHandler(config.jsonMapper());
    }

    public Single<AccessTokenModel> login(String username, String passwd) {
        Map<String, String> formParams = new HashMap<>();
        formParams.put("client_id", clientId);
        formParams.put("username", username);
        formParams.put("password", passwd);
        formParams.put("grant_type", "password");

        HttpRequest httpRequest = createRequestBuilder()
            .uri(uri.resolve(String.format("/auth/realms/%s/protocol/openid-connect/token", realm)))
            .POST(HttpRequest.BodyPublishers.ofString(transformFormUrlencoded(formParams)))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();

        log.debug("Call POST {}", httpRequest.uri());
        CompletableFuture<AccessTokenModel> future = client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenApply(responseHandler::verifyResponseAndExtractBody)
            .thenApply(s -> config.jsonMapper().decode(s, AccessTokenModel.class));

        return Single.fromFuture(future)
            .onErrorResumeNext(this::mapClientExceptions);
    }

    void waitForService() throws ServiceNotAvailableException {
        long start = System.currentTimeMillis();
        long spent = System.currentTimeMillis() - start;

        while( spent < DEFAULT_TIMEOUT ) {
            log.debug("Time spent in waiting for service {}: {} ms", uri.toString(), spent);
            try {
                log.debug( "Waiting for service to be available" );
                if( isAvailable() ) {
                    log.debug( "Service available after {} ms", spent );
                    return;
                }
            }
            catch( Exception ex ) {
                log.debug( "Exception from service: {}", ex.getMessage() );
            }

            try {
                Thread.sleep( DEFAULT_SLEEP );
            }
            catch( InterruptedException ex ) {
                log.debug( "Unable to sleep thread: " + ex.getMessage(), ex );
            }

            spent = System.currentTimeMillis() - start;
        }

        throw new ServiceNotAvailableException(String.format("The service is not available after %s ms", DEFAULT_TIMEOUT));
    }

    boolean isAvailable() throws IOException, InterruptedException {
        HttpRequest httpRequest = createRequestBuilder()
            .uri(uri)
            .GET()
            .build();

        log.debug("Call GET {}", httpRequest.uri());
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
            .version(config.httpClientVersion())
            .followRedirects(config.httpClientFollowRedirects())
            .connectTimeout(config.httpClientConnectTimeout())
            .build();
    }

    private HttpRequest.Builder createRequestBuilder() {
        return HttpRequest.newBuilder()
            .timeout(config.httpClientRequestTimeout());
    }

    private String transformFormUrlencoded(Map<String, String> map) {
        return map.keySet().stream()
            .map(key -> key + "=" + URLEncoder.encode(map.get(key), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    }

    private <T> Single<T> mapClientExceptions(Throwable throwable) {
        if( throwable instanceof ClientException) {
            return Single.error(throwable);
        }

        if( throwable.getCause() != null ) {
            return mapClientExceptions(throwable.getCause());
        }

        return Single.error(throwable);
    }

}
