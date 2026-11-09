package vn.th6.appointment.api;

import vn.th6.appointment.integration.LaboratoryClient;
import vn.th6.laboratory.contract.LabOrderResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private LaboratoryClient laboratoryClient;

    private final AtomicLong orderIds = new AtomicLong(8100);
    private String doctorToken;

    @BeforeEach
    void setUp() throws Exception {
        when(laboratoryClient.createOrder(any())).thenAnswer(invocation -> order(
                orderIds.incrementAndGet(), "RECEIVED", List.of()));
        doctorToken = issueDoctorToken();
    }

    @Test
    void createsExaminationAndProxiesResultsThroughProviderClient() throws Exception {
        mockMvc.perform(post("/api/v1/examinations")
                        .header("Authorization", "Bearer " + doctorToken)
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
                .andExpect(jsonPath("$.labOrderId").isNumber())
                .andExpect(jsonPath("$.status", is("RECEIVED")));

        when(laboratoryClient.getResults(any())).thenReturn(order(
                8101L, "COMPLETED", List.of(new LabOrderResponse.ResultItem(
                        "CBC", "Normal", "", "Normal"))));
        mockMvc.perform(get("/api/v1/examinations/results")
                        .param("appointmentId", "5001")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("COMPLETED")))
                .andExpect(jsonPath("$[0].results[0].testCode", is("CBC")));

        verify(laboratoryClient).createOrder(any());
        verify(laboratoryClient).getResults(any());
    }

    @Test
    void rejectsInactivePatientWithoutCallingProvider() throws Exception {
        mockMvc.perform(post("/api/v1/examinations")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":102,"appointmentId":5001,"testCodes":["CBC"]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")));

        verify(laboratoryClient, never()).createOrder(any());
    }

    @Test
    void requiresJwtForConsumerApi() throws Exception {
        mockMvc.perform(get("/api/v1/examinations").param("appointmentId", "5001"))
                .andExpect(status().isUnauthorized());
    }

    private String issueDoctorToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"doctor","password":"doctor-dev-only"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode token = objectMapper.readTree(response);
        return token.get("accessToken").asText();
    }

    private LabOrderResponse order(Long id, String status, List<LabOrderResponse.ResultItem> results) {
        return new LabOrderResponse(id, 101L, 5001L, List.of("CBC", "GLUCOSE"), null,
                status, results, "Lab order response", Instant.now());
    }
}
