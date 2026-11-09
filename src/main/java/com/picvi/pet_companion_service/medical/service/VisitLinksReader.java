package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.medical.domain.VisitLogRole;
import com.picvi.pet_companion_service.medical.dto.VisitLinks;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medication.api.MedicationApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reads what visits point at with one call per module for all of them, so a page of visits costs the same as one visit. */
@Component
@RequiredArgsConstructor
public class VisitLinksReader {
    private final CareApi careApi;
    private final PetLogApi petLogApi;
    private final MedicationApi medicationApi;

    public Map<UUID, VisitLinks> read(UUID petId, Collection<VetVisit> visits, LocalDate date) {
        Map<UUID, CareEventDto> careEvents = this.byId(this.careApi.events(petId, ids(visits, VetVisit::getCareEventIds)), CareEventDto::id);
        Map<UUID, LogSnapshot> logs = this.byId(this.petLogApi.snapshots(petId, logIds(visits)), LogSnapshot::logId);
        Map<UUID, MedicationApi.CourseSummary> courses = this.byId(
                this.medicationApi.courses(petId, ids(visits, VetVisit::getCourseIds), date), MedicationApi.CourseSummary::id);

        Map<UUID, VisitLinks> links = new HashMap<>();
        visits.forEach(visit -> links.put(visit.getId(), linksOf(visit, careEvents, logs, courses)));
        return links;
    }

    private static VisitLinks linksOf(VetVisit visit, Map<UUID, CareEventDto> careEvents, Map<UUID, LogSnapshot> logs,
                                      Map<UUID, MedicationApi.CourseSummary> courses) {
        List<VisitLinks.CareEventRef> careRefs = visit.getCareEventIds().stream()
                .map(careEvents::get)
                .filter(Objects::nonNull)
                .map(event -> new VisitLinks.CareEventRef(event.id(), event.itemCode(), event.performedOn()))
                .sorted(Comparator.comparing(VisitLinks.CareEventRef::performedOn).thenComparing(VisitLinks.CareEventRef::itemCode))
                .toList();
        List<VisitLinks.ClinicMedicineDto> medicines = visit.getLogs().entrySet().stream()
                .filter(link -> link.getValue() == VisitLogRole.CLINIC_MEDICINE && logs.containsKey(link.getKey()))
                .map(link -> logs.get(link.getKey()))
                .map(log -> new VisitLinks.ClinicMedicineDto(log.logId(), log.label(), log.dose()))
                .sorted(Comparator.comparing(VisitLinks.ClinicMedicineDto::label))
                .toList();
        Integer weight = visit.getLogs().entrySet().stream()
                .filter(link -> link.getValue() == VisitLogRole.WEIGHT && logs.containsKey(link.getKey()))
                .map(link -> logs.get(link.getKey()).weightGrams())
                .findFirst()
                .orElse(null);
        List<VisitLinks.CourseRef> courseRefs = visit.getCourseIds().stream()
                .map(courses::get)
                .filter(Objects::nonNull)
                .map(course -> new VisitLinks.CourseRef(course.id(), course.name(), course.status().name()))
                .sorted(Comparator.comparing(VisitLinks.CourseRef::name))
                .toList();
        return new VisitLinks(careRefs, medicines, weight, courseRefs);
    }

    private static Collection<UUID> logIds(Collection<VetVisit> visits) {
        return visits.stream().flatMap(visit -> visit.getLogs().keySet().stream()).collect(Collectors.toSet());
    }

    private static Collection<UUID> ids(Collection<VetVisit> visits, Function<VetVisit, Collection<UUID>> idsOf) {
        return visits.stream().flatMap(visit -> idsOf.apply(visit).stream()).collect(Collectors.toSet());
    }

    private <T> Map<UUID, T> byId(List<T> items, Function<T, UUID> idOf) {
        return items.stream().collect(Collectors.toMap(idOf, Function.identity()));
    }
}
