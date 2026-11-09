package vn.th6.appointment.integration;

public class IntegrationConsistencyException extends RuntimeException {
    public IntegrationConsistencyException(Long labOrderId, Throwable cause) {
        super("Examination could not be saved and lab order " + labOrderId
                + " could not be cancelled; manual reconciliation is required", cause);
    }
}
