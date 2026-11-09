package vn.th6.appointment.service;

import vn.th6.appointment.api.CreateExaminationRequest;
import vn.th6.appointment.domain.AppointmentRecord;
import vn.th6.appointment.domain.AppointmentStatus;
import vn.th6.appointment.domain.Examination;
import vn.th6.appointment.domain.Patient;
import vn.th6.appointment.integration.IntegrationConsistencyException;
import vn.th6.appointment.integration.LaboratoryClient;
import vn.th6.appointment.repository.AppointmentRepository;
import vn.th6.appointment.repository.ExaminationRepository;
import vn.th6.appointment.repository.PatientRepository;
import vn.th6.laboratory.contract.LabOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentIntegrationServiceTest {
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ExaminationRepository examinationRepository;
    @Mock
    private LaboratoryClient laboratoryClient;

    private AppointmentIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentIntegrationService(patientRepository, appointmentRepository,
                examinationRepository, laboratoryClient);
    }

    @Test
    void validatesAppointmentThenCallsLaboratoryAndStoresOrderLink() {
        when(patientRepository.findById(101L)).thenReturn(Optional.of(new Patient(101L, true)));
        when(appointmentRepository.findById(5001L)).thenReturn(Optional.of(
                new AppointmentRecord(5001L, 101L, AppointmentStatus.SCHEDULED)));
        when(laboratoryClient.createOrder(any())).thenReturn(order(8001L));
        when(examinationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createExamination(
                new CreateExaminationRequest(101L, 5001L, List.of("CBC", "GLUCOSE"), "Fasting sample"));

        ArgumentCaptor<Examination> examinationCaptor = ArgumentCaptor.forClass(Examination.class);
        verify(examinationRepository).saveAndFlush(examinationCaptor.capture());
        assertEquals(101L, examinationCaptor.getValue().getPatientId());
        assertEquals(5001L, examinationCaptor.getValue().getAppointmentId());
        assertEquals(8001L, examinationCaptor.getValue().getLabOrderId());
        verify(laboratoryClient).createOrder(any());
    }

    @Test
    void rejectsMissingPatientBeforeCallingLaboratory() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createExamination(new CreateExaminationRequest(
                        999L, 5001L, List.of("CBC"), null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(laboratoryClient, never()).createOrder(any());
    }

    @Test
    void rejectsAppointmentOwnedByAnotherPatient() {
        when(patientRepository.findById(101L)).thenReturn(Optional.of(new Patient(101L, true)));
        when(appointmentRepository.findById(5001L)).thenReturn(Optional.of(
                new AppointmentRecord(5001L, 102L, AppointmentStatus.SCHEDULED)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createExamination(new CreateExaminationRequest(
                        101L, 5001L, List.of("CBC"), null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        verify(laboratoryClient, never()).createOrder(any());
    }

    @Test
    void cancelsProviderOrderWhenLocalPersistenceFails() {
        when(patientRepository.findById(101L)).thenReturn(Optional.of(new Patient(101L, true)));
        when(appointmentRepository.findById(5001L)).thenReturn(Optional.of(
                new AppointmentRecord(5001L, 101L, AppointmentStatus.SCHEDULED)));
        when(laboratoryClient.createOrder(any())).thenReturn(order(8001L));
        when(examinationRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("simulated failure"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createExamination(new CreateExaminationRequest(
                        101L, 5001L, List.of("CBC"), null)));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(laboratoryClient).cancelOrder(8001L);
    }

    @Test
    void reportsReconciliationWhenCompensationFails() {
        when(patientRepository.findById(101L)).thenReturn(Optional.of(new Patient(101L, true)));
        when(appointmentRepository.findById(5001L)).thenReturn(Optional.of(
                new AppointmentRecord(5001L, 101L, AppointmentStatus.SCHEDULED)));
        when(laboratoryClient.createOrder(any())).thenReturn(order(8001L));
        when(examinationRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("simulated failure"));
        org.mockito.Mockito.doThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "provider down"))
                .when(laboratoryClient).cancelOrder(8001L);

        assertThrows(IntegrationConsistencyException.class,
                () -> service.createExamination(new CreateExaminationRequest(
                        101L, 5001L, List.of("CBC"), null)));
    }

    private LabOrderResponse order(Long id) {
        return new LabOrderResponse(id, 101L, 5001L, List.of("CBC"), null,
                "RECEIVED", List.of(), "Lab order created successfully", Instant.now());
    }
}
