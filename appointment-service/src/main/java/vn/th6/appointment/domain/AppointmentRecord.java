package vn.th6.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinic_appointments", schema = "appointment")
public class AppointmentRecord {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    protected AppointmentRecord() {
    }

    public AppointmentRecord(Long id, Long patientId, AppointmentStatus status) {
        this.id = id;
        this.patientId = patientId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public AppointmentStatus getStatus() {
        return status;
    }
}
