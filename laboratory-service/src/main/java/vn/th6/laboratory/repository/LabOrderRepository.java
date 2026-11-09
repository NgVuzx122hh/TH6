package vn.th6.laboratory.repository;

import vn.th6.laboratory.domain.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {
    List<LabOrder> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}
