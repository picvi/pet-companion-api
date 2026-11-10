package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.medical.domain.VisitLogRole;
import com.picvi.pet_companion_service.medical.dto.VisitLinks;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medication.api.MedicationApi;
import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitLinksReaderTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);

    @Mock
    private CareApi careApi;
    @Mock
    private PetLogApi petLogApi;
    @Mock
    private MedicationApi medicationApi;

    private VisitLinksReader reader;

    @BeforeEach
    void setUp() {
        this.reader = new VisitLinksReader(this.careApi, this.petLogApi, this.medicationApi);
    }

    @Test
    void everythingOfManyVisitsIsReadWithOneCallPerModule() {
        UUID vaccine = UUID.randomUUID();
        UUID medicineLog = UUID.randomUUID();
        UUID weightLog = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        VetVisit first = visit();
        first.getCareEventIds().add(vaccine);
        first.getLogs().put(medicineLog, VisitLogRole.CLINIC_MEDICINE);
        first.getLogs().put(weightLog, VisitLogRole.WEIGHT);
        first.getCourseIds().add(course);
        VetVisit second = visit();
        when(this.careApi.events(eq(PET), anyCollection())).thenReturn(List.of(new CareEventDto(vaccine, "DHPPI", DAY)));
        when(this.petLogApi.snapshots(eq(PET), anyCollection())).thenReturn(List.of(
                snapshot(medicineLog, LogType.MEDICINE, "Convenia", "0.5 ml", null), snapshot(weightLog, LogType.WEIGHT, null, null, 4200)));
        when(this.medicationApi.courses(eq(PET), anyCollection(), eq(DAY)))
                .thenReturn(List.of(new MedicationApi.CourseSummary(course, "Metronidazole", CourseStatus.ACTIVE)));

        Map<UUID, VisitLinks> links = this.reader.read(PET, List.of(first, second), DAY);

        VisitLinks firstLinks = links.get(first.getId());
        assertThat(firstLinks.careEvents()).containsExactly(new VisitLinks.CareEventRef(vaccine, "DHPPI", DAY));
        assertThat(firstLinks.clinicMedicines()).containsExactly(new VisitLinks.ClinicMedicineDto(medicineLog, "Convenia", "0.5 ml"));
        assertThat(firstLinks.weightGrams()).isEqualTo(4200);
        assertThat(firstLinks.courses()).containsExactly(new VisitLinks.CourseRef(course, "Metronidazole", "ACTIVE"));
        assertThat(links.get(second.getId())).isEqualTo(VisitLinks.none());
        verify(this.careApi).events(eq(PET), anyCollection());
        verify(this.petLogApi).snapshots(eq(PET), anyCollection());
    }

    @Test
    void anItemThatIsGoneIsLeftOut() {
        VetVisit visit = visit();
        visit.getCareEventIds().add(UUID.randomUUID());
        visit.getLogs().put(UUID.randomUUID(), VisitLogRole.WEIGHT);
        visit.getCourseIds().add(UUID.randomUUID());
        when(this.careApi.events(eq(PET), anyCollection())).thenReturn(List.of());
        when(this.petLogApi.snapshots(eq(PET), anyCollection())).thenReturn(List.of());
        when(this.medicationApi.courses(eq(PET), anyCollection(), eq(DAY))).thenReturn(List.of());

        assertThat(this.reader.read(PET, List.of(visit), DAY).get(visit.getId())).isEqualTo(VisitLinks.none());
    }

    private static VetVisit visit() {
        VetVisit visit = VetVisit.done(PET, UUID.randomUUID(), DAY, "VetCare", Instant.parse("2026-10-22T10:00:00Z"));
        ReflectionTestUtils.setField(visit, "id", UUID.randomUUID());
        return visit;
    }

    private static LogSnapshot snapshot(UUID id, LogType type, String label, String dose, Integer weight) {
        return new LogSnapshot(id, type, null, null, null, Set.of(), DAY, 600, weight, null, label, dose);
    }
}
