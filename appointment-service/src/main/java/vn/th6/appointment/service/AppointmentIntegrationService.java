package vn.th6.appointment.service;

import vn.th6.appointment.api.CreateExaminationRequest;
import vn.th6.appointment.domain.AppointmentRecord;
import vn.th6.appointment.domain.AppointmentStatus;
import vn.th6.appointment.domain.Examination;
import vn.th6.appointment.integration.IntegrationConsistencyException;
import vn.th6.appointment.integration.LaboratoryClient;
import vn.th6.appointment.repository.AppointmentRepository;
import vn.th6.appointment.repository.ExaminationRepository;
import vn.th6.appointment.repository.PatientRepository;
import vn.th6.laboratory.contract.LabOrderRequest;
import vn.th6.laboratory.contract.LabOrderResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class AppointmentIntegrationService {
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ExaminationRepository examinationRepository;
    private final LaboratoryClient laboratoryClient;

    public AppointmentIntegrationService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            ExaminationRepository examinationRepository,
            LaboratoryClient laboratoryClient) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.examinationRepository = examinationRepository;
        this.laboratoryClient = laboratoryClient;
    }

    public ExaminationCreation createExamination(CreateExaminationRequest request) {
        var patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Patient not found"));
        if (!patient.isActive()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Patient is not active");
        }

        AppointmentRecord appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Appointment not found"));
        if (!appointment.getPatientId().equals(patient.getId())) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Appointment does not belong to the specified patient");
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Appointment is not valid for examination");
        }

        LabOrderResponse labOrder = laboratoryClient.createOrder(new LabOrderRequest(
                patient.getId(), appointment.getId(), request.testCodes(), request.notes()));
        try {
            Examination examination = examinationRepository.saveAndFlush(
                    new Examination(patient.getId(), appointment.getId(), labOrder.orderId()));
            return new ExaminationCreation(examination, labOrder);
        } catch (DataAccessException persistenceFailure) {
            try {
                laboratoryClient.cancelOrder(labOrder.orderId());
            } catch (ResponseStatusException compensationFailure) {
                throw new IntegrationConsistencyException(labOrder.orderId(), compensationFailure);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Examination could not be saved; the lab order was cancelled", persistenceFailure);
        }
    }

    public List<Examination> findByAppointment(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Appointment not found");
        }
        return examinationRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId);
    }

    public Examination findExamination(Long examinationId) {
        return examinationRepository.findById(examinationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Examination not found"));
    }

    public LabOrderResponse getOrder(Long labOrderId) {
        return laboratoryClient.getOrder(labOrderId);
    }

    public List<LabOrderResponse> getResultsByAppointment(Long appointmentId) {
        return findByAppointment(appointmentId).stream()
                .map(examination -> laboratoryClient.getResults(examination.getLabOrderId()))
                .toList();
    }

    public LabOrderResponse getResultsByLabOrder(Long labOrderId) {
        examinationRepository.findByLabOrderId(labOrderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lab order is not linked to an examination"));
        return laboratoryClient.getResults(labOrderId);
    }
}
