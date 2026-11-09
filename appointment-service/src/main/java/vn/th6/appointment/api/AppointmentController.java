package vn.th6.appointment.api;

import vn.th6.appointment.domain.Examination;
import vn.th6.appointment.service.ExaminationCreation;
import vn.th6.appointment.service.AppointmentIntegrationService;
import vn.th6.laboratory.contract.LabOrderResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {
    private final AppointmentIntegrationService integrationService;

    public AppointmentController(AppointmentIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping("/examinations")
    public ResponseEntity<ExaminationResponse> createExamination(
            @Valid @RequestBody CreateExaminationRequest request) {
        ExaminationCreation creation = integrationService.createExamination(request);
        Examination examination = creation.examination();
        ExaminationResponse response = ExaminationResponse.from(examination, creation.labOrder());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(examination.getId()).toUri())
                .body(response);
    }

    @GetMapping("/examinations/{examinationId}")
    public ExaminationResponse getExamination(@PathVariable("examinationId") Long examinationId) {
        Examination examination = integrationService.findExamination(examinationId);
        LabOrderResponse order = integrationService.getOrder(examination.getLabOrderId());
        return ExaminationResponse.from(examination, order);
    }

    @GetMapping("/examinations")
    public List<ExaminationResponse> findExaminations(@RequestParam("appointmentId") Long appointmentId) {
        return integrationService.findByAppointment(appointmentId).stream()
                .map(examination -> {
                    LabOrderResponse order = integrationService.getOrder(examination.getLabOrderId());
                    return ExaminationResponse.from(examination, order);
                })
                .toList();
    }

    @GetMapping("/examinations/results")
    public List<LabOrderResponse> getResultsByAppointment(@RequestParam("appointmentId") Long appointmentId) {
        return integrationService.getResultsByAppointment(appointmentId);
    }

    @GetMapping("/examinations/lab-orders/{labOrderId}/results")
    public LabOrderResponse getResultsByLabOrder(@PathVariable("labOrderId") Long labOrderId) {
        return integrationService.getResultsByLabOrder(labOrderId);
    }
}
