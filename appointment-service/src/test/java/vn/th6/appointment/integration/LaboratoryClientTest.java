package vn.th6.appointment.integration;

import vn.th6.laboratory.contract.LabOrderRequest;
import vn.th6.laboratory.contract.LabOrderResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LaboratoryClientTest {
    private MockRestServiceServer server;
    private LaboratoryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LaboratoryClient(builder.baseUrl("http://laboratory.test").build());
        Jwt jwt = Jwt.withTokenValue("doctor-access-token")
                .header("alg", "HS256")
                .subject("doctor")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("roles", List.of("ROLE_DOCTOR"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void postsOrderToProviderAndForwardsAuthenticatedUsersJwt() {
        server.expect(requestTo("http://laboratory.test/api/v1/lab-orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AUTHORIZATION, "Bearer doctor-access-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"patientId":101,"appointmentId":5001,"testCodes":["CBC"],"notes":"Fasting"}
                        """))
                .andRespond(withSuccess("""
                        {
                          "orderId":8001,
                          "patientId":101,
                          "appointmentId":5001,
                          "testCodes":["CBC"],
                          "status":"RECEIVED",
                          "results":[],
                          "message":"Lab order created successfully",
                          "createdAt":"2026-01-01T10:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        LabOrderResponse response = client.createOrder(
                new LabOrderRequest(101L, 5001L, List.of("CBC"), "Fasting"));

        assertEquals(8001L, response.orderId());
        assertEquals("RECEIVED", response.status());
        server.verify();
    }

    @Test
    void mapsProviderConflictToConsumerConflict() {
        server.expect(requestTo("http://laboratory.test/api/v1/lab-orders/8001/results"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer doctor-access-token"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"CONFLICT\",\"message\":\"Results pending\"}"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> client.getResults(8001L));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, exception.getStatusCode());
        server.verify();
    }

    @Test
    void mapsProviderTimeoutToServiceUnavailable() {
        server.expect(requestTo("http://laboratory.test/api/v1/lab-orders"))
                .andRespond(request -> {
                    throw new ResourceAccessException("simulated timeout");
                });

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> client.createOrder(new LabOrderRequest(101L, 5001L, List.of("CBC"), null)));

        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        server.verify();
    }
}
