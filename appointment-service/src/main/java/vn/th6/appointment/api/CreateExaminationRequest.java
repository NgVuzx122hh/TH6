package vn.th6.appointment.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateExaminationRequest(
        @NotNull Long patientId,
        @NotNull Long appointmentId,
        @NotEmpty List<@NotEmpty String> testCodes,
        @Size(max = 1000) String notes) {
}
