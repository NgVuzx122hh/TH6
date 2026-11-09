package vn.th6.laboratory.api;

import vn.th6.laboratory.domain.LabOrder;
import vn.th6.laboratory.domain.LabOrderStatus;
import vn.th6.laboratory.domain.LabResult;

import java.time.Instant;
import java.util.List;

public record LabOrderResponse(
        Long orderId,
        Long patientId,
        Long appointmentId,
        List<String> testCodes,
        String notes,
        LabOrderStatus status,
        List<ResultItem> results,
        String message,
        Instant createdAt) {

    public static LabOrderResponse from(LabOrder order) {
        return from(order, "Lab order retrieved successfully");
    }

    public static LabOrderResponse from(LabOrder order, String message) {
        List<ResultItem> results = order.getResults().stream()
                .map(result -> new ResultItem(result.getTestCode(), result.getValue(),
                        result.getUnit(), result.getReferenceRange()))
                .toList();
        return new LabOrderResponse(order.getId(), order.getPatientId(), order.getAppointmentId(),
                order.getTestCodes(), order.getNotes(), order.getStatus(), results,
                message,
                order.getCreatedAt());
    }

    public record ResultItem(String testCode, String value, String unit, String referenceRange) {
        public static ResultItem from(LabResult result) {
            return new ResultItem(result.getTestCode(), result.getValue(), result.getUnit(),
                    result.getReferenceRange());
        }
    }
}
