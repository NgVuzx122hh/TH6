package vn.th6.laboratory.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record LabResultsRequest(@NotEmpty List<@Valid ResultItem> results) {
    public record ResultItem(
            @NotBlank String testCode,
            @NotBlank String value,
            String unit,
            String referenceRange) {
    }
}
