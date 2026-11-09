package vn.th6.laboratory.contract;

import java.util.List;

public record LabOrderRequest(Long patientId, Long appointmentId, List<String> testCodes, String notes) {
    public LabOrderRequest {
        testCodes = List.copyOf(testCodes);
    }
}
