package vn.th6.appointment.repository;

import vn.th6.appointment.domain.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<AppointmentRecord, Long> {
}
