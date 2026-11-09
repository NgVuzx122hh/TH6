package vn.th6.laboratory.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:labdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS laboratory",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class LaboratoryControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsOrderAndMakesCompletedResultsAvailableThroughApi() throws Exception {
        String response = mockMvc.perform(post("/api/v1/lab-orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 101,
                                  "appointmentId": 5001,
                                  "testCodes": ["CBC", "GLUCOSE"],
                                  "notes": "Fasting sample"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("RECEIVED")))
                .andExpect(jsonPath("$.testCodes", containsInAnyOrder("CBC", "GLUCOSE")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long orderId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("orderId").asLong();

        mockMvc.perform(get("/api/v1/lab-orders/{id}/results", orderId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));

        mockMvc.perform(put("/api/v1/lab-orders/{id}/results", orderId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LAB_TECHNICIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "results": [
                                    {"testCode": "CBC", "value": "Normal", "unit": "", "referenceRange": "Normal"},
                                    {"testCode": "GLUCOSE", "value": "92", "unit": "mg/dL", "referenceRange": "70-99"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        mockMvc.perform(get("/api/v1/lab-orders/{id}/results", orderId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()", is(2)))
                .andExpect(jsonPath("$.results[1].value").exists());
    }

    @Test
    void rejectsUnknownTestCodes() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":101,"appointmentId":5001,"testCodes":["UNKNOWN"]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_TEST")));
    }

    @Test
    void rejectsMalformedOrder() throws Exception {
        mockMvc.perform(post("/api/v1/lab-orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":101,"appointmentId":5001,"testCodes":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void issuesJwtAndRequiresAuthenticationForProviderApi() throws Exception {
        mockMvc.perform(get("/api/v1/lab-tests"))
                .andExpect(status().isUnauthorized());

        String response = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"doctor","password":"doctor-dev-only"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/lab-tests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void preventsDoctorFromWritingLabResults() throws Exception {
        mockMvc.perform(put("/api/v1/lab-orders/1/results")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"results":[{"testCode":"CBC","value":"Normal"}]}
                                """))
                .andExpect(status().isForbidden());
    }
}
