package vn.th6.appointment.integration;

import vn.th6.laboratory.contract.LabOrderRequest;
import vn.th6.laboratory.contract.LabOrderResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class LaboratoryClient {
    private final RestClient restClient;

    public LaboratoryClient(RestClient laboratoryRestClient) {
        this.restClient = laboratoryRestClient;
    }

    public LabOrderResponse createOrder(LabOrderRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/lab-orders")
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .body(request)
                    .retrieve()
                    .body(LabOrderResponse.class);
        } catch (RestClientResponseException exception) {
            throw translateProviderError(exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Laboratory Service timed out or is unavailable", exception);
        }
    }

    public LabOrderResponse getResults(Long labOrderId) {
        try {
            return restClient.get()
                    .uri("/api/v1/lab-orders/{id}/results", labOrderId)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .body(LabOrderResponse.class);
        } catch (RestClientResponseException exception) {
            throw translateProviderError(exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Laboratory Service timed out or is unavailable", exception);
        }
    }

    public LabOrderResponse getOrder(Long labOrderId) {
        try {
            return restClient.get()
                    .uri("/api/v1/lab-orders/{id}", labOrderId)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .body(LabOrderResponse.class);
        } catch (RestClientResponseException exception) {
            throw translateProviderError(exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Laboratory Service timed out or is unavailable", exception);
        }
    }

    public void cancelOrder(Long labOrderId) {
        try {
            restClient.delete()
                    .uri("/api/v1/lab-orders/{id}", labOrderId)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw translateProviderError(exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Could not confirm cancellation with Laboratory Service", exception);
        }
    }

    private ResponseStatusException translateProviderError(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.is4xxClientError()) {
            String reason = exception.getResponseBodyAsString();
            return new ResponseStatusException(status, reason.isBlank()
                    ? "Laboratory Service rejected the request" : reason, exception);
        }
        return new ResponseStatusException(BAD_GATEWAY,
                "Laboratory Service returned an unexpected error", exception);
    }

    private String accessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                "A valid user token is required for Laboratory Service access");
    }
}
