package vn.th6.laboratory.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lab_orders", schema = "laboratory")
public class LabOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long appointmentId;

    @ElementCollection
    @CollectionTable(name = "lab_order_tests", schema = "laboratory",
            joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "test_code", nullable = false)
    private List<String> testCodes = new ArrayList<>();

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabOrderStatus status;

    @ElementCollection
    @CollectionTable(name = "lab_order_results", schema = "laboratory",
            joinColumns = @JoinColumn(name = "order_id"))
    private List<LabResult> results = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    protected LabOrder() {
    }

    public LabOrder(Long patientId, Long appointmentId, List<String> testCodes, String notes) {
        this.patientId = patientId;
        this.appointmentId = appointmentId;
        this.testCodes = new ArrayList<>(testCodes);
        this.notes = notes;
        this.status = LabOrderStatus.RECEIVED;
        this.createdAt = Instant.now();
    }

    public void complete(List<LabResult> results) {
        this.results = new ArrayList<>(results);
        this.status = LabOrderStatus.COMPLETED;
    }

    public void cancel() {
        this.status = LabOrderStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public List<String> getTestCodes() {
        return List.copyOf(testCodes);
    }

    public String getNotes() {
        return notes;
    }

    public LabOrderStatus getStatus() {
        return status;
    }

    public List<LabResult> getResults() {
        return List.copyOf(results);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
