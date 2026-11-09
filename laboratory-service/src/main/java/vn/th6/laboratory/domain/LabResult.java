package vn.th6.laboratory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LabResult {
    @Column(name = "test_code", nullable = false)
    private String testCode;

    @Column(name = "result_value", nullable = false)
    private String value;

    @Column(name = "unit")
    private String unit;

    @Column(name = "reference_range")
    private String referenceRange;

    protected LabResult() {
    }

    public LabResult(String testCode, String value, String unit, String referenceRange) {
        this.testCode = testCode;
        this.value = value;
        this.unit = unit;
        this.referenceRange = referenceRange;
    }

    public String getTestCode() {
        return testCode;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public String getReferenceRange() {
        return referenceRange;
    }
}
