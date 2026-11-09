package vn.th6.laboratory.service;

import vn.th6.laboratory.api.LabOrderRequest;
import vn.th6.laboratory.api.LabOrderResponse;
import vn.th6.laboratory.api.LabResultsRequest;
import vn.th6.laboratory.domain.LabOrder;
import vn.th6.laboratory.domain.LabOrderStatus;
import vn.th6.laboratory.domain.LabResult;
import vn.th6.laboratory.repository.LabOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class LaboratoryService {
    private static final Map<String, String> TESTS = Map.of(
            "CBC", "Complete Blood Count",
            "GLUCOSE", "Blood Glucose");

    private final LabOrderRepository orderRepository;

    public LaboratoryService(LabOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Map<String, String>> listTests() {
        return TESTS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.of("code", entry.getKey(), "name", entry.getValue()))
                .toList();
    }

    @Transactional
    public LabOrderResponse createOrder(LabOrderRequest request) {
        List<String> normalizedCodes = request.testCodes().stream()
                .map(code -> code.trim().toUpperCase())
                .distinct()
                .toList();
        List<String> unknownCodes = normalizedCodes.stream()
                .filter(code -> !TESTS.containsKey(code))
                .toList();
        if (!unknownCodes.isEmpty()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Unknown test code(s): " + String.join(", ", unknownCodes));
        }
        return LabOrderResponse.from(orderRepository.save(new LabOrder(request.patientId(),
                request.appointmentId(), normalizedCodes, request.notes())),
                "Lab order created successfully");
    }

    @Transactional(readOnly = true)
    public LabOrderResponse getOrder(Long id) {
        return LabOrderResponse.from(findOrder(id));
    }

    @Transactional(readOnly = true)
    public List<LabOrderResponse> findByAppointment(Long appointmentId) {
        return orderRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId).stream()
                .map(LabOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LabOrderResponse getResults(Long id) {
        LabOrder order = findOrder(id);
        if (order.getStatus() != LabOrderStatus.COMPLETED) {
            throw new ResponseStatusException(CONFLICT, "Lab results are not available yet");
        }
        return LabOrderResponse.from(order, "Lab results retrieved successfully");
    }

    @Transactional
    public LabOrderResponse completeOrder(Long id, LabResultsRequest request) {
        LabOrder order = findOrder(id);
        if (order.getStatus() != LabOrderStatus.RECEIVED) {
            throw new ResponseStatusException(CONFLICT, "Only a received order can be completed");
        }
        Set<String> requestedCodes = Set.copyOf(order.getTestCodes());
        List<LabResult> results = request.results().stream()
                .map(result -> new LabResult(result.testCode().trim().toUpperCase(), result.value().trim(),
                        result.unit(), result.referenceRange()))
                .toList();
        Set<String> resultCodes = results.stream().map(LabResult::getTestCode)
                .collect(java.util.stream.Collectors.toSet());
        if (results.size() != requestedCodes.size() || !resultCodes.equals(requestedCodes)) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Results must contain exactly the tests requested by the order");
        }
        order.complete(results);
        return LabOrderResponse.from(order, "Lab results recorded successfully");
    }

    @Transactional
    public void cancelOrder(Long id) {
        LabOrder order = findOrder(id);
        if (order.getStatus() != LabOrderStatus.RECEIVED) {
            throw new ResponseStatusException(CONFLICT, "Only a received order can be cancelled");
        }
        order.cancel();
    }

    private LabOrder findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lab order not found"));
    }
}
