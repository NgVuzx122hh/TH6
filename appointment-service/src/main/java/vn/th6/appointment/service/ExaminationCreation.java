package vn.th6.appointment.service;

import vn.th6.appointment.domain.Examination;
import vn.th6.laboratory.contract.LabOrderResponse;

public record ExaminationCreation(Examination examination, LabOrderResponse labOrder) {
}
