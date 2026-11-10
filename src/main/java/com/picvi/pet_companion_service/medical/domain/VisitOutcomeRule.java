package com.picvi.pet_companion_service.medical.domain;

import java.util.EnumSet;
import java.util.Set;

public class VisitOutcomeRule {
    public Set<VisitOutcome> derive(boolean justCheck, boolean hasTreatment, boolean hasTests, boolean hasFollowUp) {
        Set<VisitOutcome> outcomes = EnumSet.noneOf(VisitOutcome.class);
        if (justCheck) {
            outcomes.add(VisitOutcome.FINE);
        }
        if (hasTreatment) {
            outcomes.add(VisitOutcome.TREATMENT);
        }
        if (hasTests) {
            outcomes.add(VisitOutcome.TESTS);
        }
        if (hasFollowUp) {
            outcomes.add(VisitOutcome.BACK);
        }
        return outcomes;
    }
}
