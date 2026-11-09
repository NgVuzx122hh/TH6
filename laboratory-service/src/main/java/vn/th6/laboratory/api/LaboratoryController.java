package vn.th6.laboratory.api;

import vn.th6.laboratory.service.LaboratoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class LaboratoryController {
    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    @GetMapping("/lab-tests")
    public List<java.util.Map<String, String>> listTests() {
        return laboratoryService.listTests();
    }

    @PostMapping("/lab-orders")
    public ResponseEntity<LabOrderResponse> createOrder(@Valid @RequestBody LabOrderRequest request) {
        LabOrderResponse response = laboratoryService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/lab-orders")
    public List<LabOrderResponse> findOrders(@RequestParam("appointmentId") Long appointmentId) {
        return laboratoryService.findByAppointment(appointmentId);
    }

    @GetMapping("/lab-orders/{id}")
    public LabOrderResponse getOrder(@PathVariable("id") Long id) {
        return laboratoryService.getOrder(id);
    }

    @GetMapping("/lab-orders/{id}/results")
    public LabOrderResponse getResults(@PathVariable("id") Long id) {
        return laboratoryService.getResults(id);
    }

    @PutMapping("/lab-orders/{id}/results")
    public LabOrderResponse completeOrder(
            @PathVariable("id") Long id, @Valid @RequestBody LabResultsRequest request) {
        return laboratoryService.completeOrder(id, request);
    }

    @DeleteMapping("/lab-orders/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable("id") Long id) {
        laboratoryService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

}
