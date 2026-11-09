package vn.th6.appointment.api;

import vn.th6.appointment.domain.Examination;
import vn.th6.laboratory.contract.LabOrderResponse;

import java.time.Instant;
import java.util.List;

public record ExaminationResponse(
        Long examinationId,
        Long patientId,
        Long appointmentId,
        Long labOrderId,
        String status,
        List<String> testCodes,
        Instant createdAt) {

    public static ExaminationResponse from(Examination examination, LabOrderResponse order) {
        return new ExaminationResponse(examination.getId(), examination.getPatientId(),
                examination.getAppointmentId(), examination.getLabOrderId(), order.status(),
                order.testCodes(), examination.getCreatedAt());
    }
}
