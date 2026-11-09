package vn.th6.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "examinations", schema = "appointment")
public class Examination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long appointmentId;

    @Column(nullable = false, unique = true)
    private Long labOrderId;

    @Column(nullable = false)
    private Instant createdAt;

    protected Examination() {
    }

    public Examination(Long patientId, Long appointmentId, Long labOrderId) {
        this.patientId = patientId;
        this.appointmentId = appointmentId;
        this.labOrderId = labOrderId;
        this.createdAt = Instant.now();
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

    public Long getLabOrderId() {
        return labOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
