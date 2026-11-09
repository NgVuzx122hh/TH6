package vn.th6.laboratory.contract;

import java.time.Instant;
import java.util.List;

public record LabOrderResponse(
        Long orderId,
        Long patientId,
        Long appointmentId,
        List<String> testCodes,
        String notes,
        String status,
        List<ResultItem> results,
        String message,
        Instant createdAt) {
    public record ResultItem(String testCode, String value, String unit, String referenceRange) {
    }
}
