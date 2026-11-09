package vn.th6.appointment.config;

import vn.th6.appointment.domain.AppointmentRecord;
import vn.th6.appointment.domain.AppointmentStatus;
import vn.th6.appointment.domain.Patient;
import vn.th6.appointment.repository.AppointmentRepository;
import vn.th6.appointment.repository.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleDataConfiguration {
    @Bean
    CommandLineRunner seedSampleData(
            PatientRepository patientRepository, AppointmentRepository appointmentRepository) {
        return args -> {
            if (!patientRepository.existsById(101L)) {
                patientRepository.save(new Patient(101L, true));
            }
            if (!patientRepository.existsById(102L)) {
                patientRepository.save(new Patient(102L, false));
            }
            if (!appointmentRepository.existsById(5001L)) {
                appointmentRepository.save(new AppointmentRecord(
                        5001L, 101L, AppointmentStatus.SCHEDULED));
            }
            if (!appointmentRepository.existsById(5002L)) {
                appointmentRepository.save(new AppointmentRecord(
                        5002L, 101L, AppointmentStatus.COMPLETED));
            }
        };
    }
}
