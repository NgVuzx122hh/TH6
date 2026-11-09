package vn.th6.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patients", schema = "appointment")
public class Patient {
    @Id
    private Long id;

    @Column(nullable = false)
    private boolean active;

    protected Patient() {
    }

    public Patient(Long id, boolean active) {
        this.id = id;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }
}
