package vn.th6.appointment.repository;

import vn.th6.appointment.domain.Examination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    Optional<Examination> findByLabOrderId(Long labOrderId);
}
