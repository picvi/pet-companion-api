package com.picvi.pet_companion_service.medical.domain;

import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.ClinicMedicine;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.Procedure;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.TestRequest;
import org.junit.jupiter.api.Test;


import static com.picvi.pet_companion_service.medical.LogVisitRequests.TODAY;
import static com.picvi.pet_companion_service.medical.LogVisitRequests.aCourse;
import static com.picvi.pet_companion_service.medical.LogVisitRequests.aVisit;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VetVisitRulesTest {
    private final VetVisitRules rules = new VetVisitRules();

    @Test
    void aFullVisitIsValid() {
        LogVisitRequest request = aVisit()
                .careItemCodes("DHPPI")
                .clinicMedicines(new ClinicMedicine("Convenia", "0.5 ml"))
                .homeCourses(aCourse("Metronidazole"))
                .weightGrams(4200)
                .tests(new TestRequest(VisitTestKind.BLOOD, true))
                .procedure(new Procedure(VisitProcedureKind.DENTAL, null))
                .followUp(TODAY.plusDays(7), 600)
                .build();

        assertThatCode(() -> this.rules.validateLog(request)).doesNotThrowAnyException();
    }

    @Test
    void aVisitInTheFutureIsRejected() {
        assertInvalid(aVisit().visitedOn(TODAY.plusDays(1)).build(), "visitedOn", InvalidVetVisit.OUT_OF_RANGE);
    }

    @Test
    void theVisitMinuteMustBeInTheDay() {
        assertInvalid(aVisit().visitMinute(1440).build(), "visitMinute", InvalidVetVisit.OUT_OF_RANGE);
    }

    @Test
    void justCheckAllowsNoItems() {
        assertInvalid(aVisit().justCheck().careItemCodes("DHPPI").build(), "justCheck", InvalidVetVisit.INVALID);
        assertInvalid(aVisit().justCheck().procedure(new Procedure(VisitProcedureKind.DENTAL, null)).build(), "justCheck", InvalidVetVisit.INVALID);
        assertThatCode(() -> this.rules.validateLog(aVisit().justCheck().followUp(TODAY, 900).build())).doesNotThrowAnyException();
    }

    @Test
    void atMostFiveClinicMedicinesAndThreeHomeCourses() {
        ClinicMedicine medicine = new ClinicMedicine("Convenia", null);
        assertInvalid(aVisit().clinicMedicines(medicine, medicine, medicine, medicine, medicine, medicine).build(), "clinicMedicines", InvalidVetVisit.TOO_MANY);
        assertInvalid(aVisit().homeCourses(aCourse("A"), aCourse("B"), aCourse("C"), aCourse("D")).build(), "homeCourses", InvalidVetVisit.TOO_MANY);
    }

    @Test
    void aClinicMedicineNeedsALabel() {
        assertInvalid(aVisit().clinicMedicines(new ClinicMedicine(" ", "1 ml")).build(), "clinicMedicines", InvalidVetVisit.REQUIRED);
    }

    @Test
    void aTestKindCanBeListedOnce() {
        assertInvalid(aVisit().tests(new TestRequest(VisitTestKind.BLOOD, true), new TestRequest(VisitTestKind.BLOOD, false)).build(), "tests", InvalidVetVisit.INVALID);
    }

    @Test
    void anotherProcedureNeedsANote() {
        assertInvalid(aVisit().procedure(new Procedure(VisitProcedureKind.OTHER, " ")).build(), "procedure.note", InvalidVetVisit.REQUIRED);
    }

    @Test
    void theFollowUpCannotBeBeforeTheVisitButMayBeLaterTheSameDay() {
        assertInvalid(aVisit().followUp(TODAY.minusDays(1), 600).build(), "followUp.on", InvalidVetVisit.OUT_OF_RANGE);
        assertThatCode(() -> this.rules.validateLog(aVisit().followUp(TODAY, 1200).build())).doesNotThrowAnyException();
    }

    @Test
    void theFollowUpMinuteIsInTheDay() {
        assertInvalid(aVisit().followUp(TODAY.plusDays(3), 1400).build(), "followUp.minute", InvalidVetVisit.OUT_OF_RANGE);
    }

    @Test
    void longTextsAreRejected() {
        assertInvalid(aVisit().note("x".repeat(VetVisitRules.MAX_NOTE_LENGTH + 1)).build(), "note", InvalidVetVisit.TOO_LONG);
        assertInvalid(aVisit().clinic("x".repeat(VetVisitRules.MAX_CLINIC_LENGTH + 1)).build(), "clinic", InvalidVetVisit.TOO_LONG);
        assertInvalid(aVisit().procedure(new Procedure(VisitProcedureKind.OTHER, "x".repeat(VetVisitRules.MAX_PROCEDURE_NOTE_LENGTH + 1))).build(),
                "procedure.note", InvalidVetVisit.TOO_LONG);
    }

    private void assertInvalid(LogVisitRequest request, String field, String reason) {
        assertThatThrownBy(() -> this.rules.validateLog(request))
                .isInstanceOf(InvalidVetVisit.class)
                .hasMessage(field + ": " + reason);
    }
}
