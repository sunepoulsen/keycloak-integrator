package dk.sunepoulsen.keycloak.integrator

import dk.sunepoulsen.keycloak.integrator.model.AccessTokenModel
import dk.sunepoulsen.tes.springboot.client.core.rs.exceptions.ClientUnauthorizedException
import dk.sunepoulsen.tes.springboot.ct.core.docker.DockerDeployment
import spock.lang.Specification

class KeycloakIntegratorSpec extends Specification {

    private static DockerDeployment deployment

    void setupSpec() {
        deployment = new DockerDeployment('ut', [])
        deployment.deploy()

        URI uri = deployment.httpUriForContainer('keycloak-service')
        new KeycloakIntegrator(uri, 'master', 'admin-cli').waitForService()
    }

    void cleanupSpec() {
        deployment.undeploy()
    }

    void "Login into keycloak with valid credentials"() {
        given:
            URI uri = deployment.httpUriForContainer('keycloak-service')
            KeycloakIntegrator sut = new KeycloakIntegrator(uri, 'master', 'admin-cli')

        when:
            AccessTokenModel result = sut.login('admin', 'admin').blockingGet()

        then:
            !result.accessToken.empty
    }

    void "Login into keycloak with bad credentials"() {
        given:
            URI uri = deployment.httpUriForContainer('keycloak-service')
            KeycloakIntegrator sut = new KeycloakIntegrator(uri, 'master', 'admin-cli')

        when:
            sut.login('wrong', 'wrong').blockingGet()

        then:
            ClientUnauthorizedException ex = thrown(ClientUnauthorizedException)
            ex.response.statusCode() == 401
            ex.serviceError.code == 'invalid_grant'
            ex.serviceError.message == 'Invalid user credentials'
    }

}
