package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.exception.medical.VetVisitNotFound;
import com.picvi.pet_companion_service.exception.medical.VisitStatusConflict;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.medical.domain.PlannedAppointment;
import com.picvi.pet_companion_service.medical.domain.VetAppointmentRule;
import com.picvi.pet_companion_service.medical.domain.VetVisitRules;
import com.picvi.pet_companion_service.medical.domain.VisitChildIds;
import com.picvi.pet_companion_service.medical.domain.VisitLogRole;
import com.picvi.pet_companion_service.medical.domain.VisitOutcomeRule;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.domain.VisitTestKind;
import com.picvi.pet_companion_service.medical.dto.LastVisitDto;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest;
import com.picvi.pet_companion_service.medical.dto.NextVetAppointmentDto;
import com.picvi.pet_companion_service.medical.dto.PlanVisitRequest;
import com.picvi.pet_companion_service.medical.dto.UpcomingVisitsResponse;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.dto.VisitLinks;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.entity.VisitProcedure;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import com.picvi.pet_companion_service.medication.api.MedicationApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class VetVisitService {
    private static final int DEFAULT_VISIT_MINUTE = 720;

    public record CreateResult(VetVisitDto visit, boolean created) {
    }

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final VetVisitRepository repository;
    private final VetVisitRules rules;
    private final VisitOutcomeRule outcomeRule;
    private final DayEventApi dayEventApi;
    private final CareApi careApi;
    private final PetLogApi petLogApi;
    private final PetLogRecorder petLogRecorder;
    private final MedicationApi medicationApi;
    private final VisitLinksReader linksReader;
    private final VetAppointmentRule appointmentRule;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UpcomingVisitsResponse upcoming(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        List<VetVisit> plans = this.repository.findAllByPetIdAndStatusAndVisitedOnGreaterThanEqualOrderByVisitedOnAscScheduledMinuteAsc(
                petId, VisitStatus.PLANNED, date.minusDays(VetAppointmentRule.PASSED_VISIBLE_DAYS));
        Map<UUID, DayEventApi.VetAppointment> appointments = this.dayEventApi.vetAppointments(petId,
                plans.stream().map(VetVisit::getDayEventId).filter(Objects::nonNull).toList());
        List<PlannedAppointment> booked = plans.stream()
                .filter(plan -> appointments.containsKey(plan.getDayEventId()))
                .map(plan -> planned(plan, appointments.get(plan.getDayEventId())))
                .toList();
        LastVisitDto lastVisit = this.repository
                .findFirstByPetIdAndStatusAndVisitedOnLessThanEqualOrderByVisitedOnDescCreatedAtDesc(petId, VisitStatus.DONE, date)
                .map(LastVisitDto::of)
                .orElse(null);
        return new UpcomingVisitsResponse(date,
                plans.stream().map(plan -> VetVisitDto.of(plan, appointments.get(plan.getDayEventId()), VisitLinks.none(), null)).toList(),
                lastVisit,
                this.appointmentRule.evaluate(booked, date).map(NextVetAppointmentDto::of).orElse(null));
    }

    @Transactional(readOnly = true)
    public VetVisitDto get(UUID ownerId, UUID petId, UUID visitId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        return this.toDto(this.find(petId, visitId), LocalDate.now(this.clock));
    }

    public CreateResult plan(UUID ownerId, UUID petId, PlanVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        UUID clientId = this.requireClientId(request.clientId());
        Optional<VetVisit> existing = this.repository.findByPetIdAndClientId(petId, clientId);
        if (existing.isPresent()) {
            return new CreateResult(this.toDto(existing.get(), request.date()), false);
        }
        String clinic = blankToNull(request.clinic());
        String prepNote = blankToNull(request.prepNote());
        this.rules.validatePlan(request.scheduledOn(), request.scheduledMinute(), clinic, prepNote, request.date());
        VetVisit visit = VetVisit.planned(petId, clientId, request.scheduledOn(), request.scheduledMinute(), clinic,
                prepNote, this.clock.instant());
        visit.setDayEventId(this.dayEventApi.scheduleVet(petId, request.scheduledOn(), request.scheduledMinute(), request.date()));
        return new CreateResult(this.toDto(this.repository.saveAndFlush(visit), request.date()), true);
    }

    public VetVisitDto updatePlan(UUID ownerId, UUID petId, UUID visitId, long expectedVersion, PlanVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        VetVisit visit = this.find(petId, visitId);
        this.requireStatus(visit, VisitStatus.PLANNED, expectedVersion);
        String clinic = blankToNull(request.clinic());
        String prepNote = blankToNull(request.prepNote());
        this.rules.validatePlan(request.scheduledOn(), request.scheduledMinute(), clinic, prepNote, request.date());
        visit.replan(request.scheduledOn(), request.scheduledMinute(), clinic, prepNote, this.clock.instant());
        this.syncEvent(petId, visit, request.scheduledOn(), request.scheduledMinute(), request.date());
        return this.toDto(this.repository.saveAndFlush(visit), request.date());
    }

    public CreateResult log(UUID ownerId, UUID petId, LogVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        UUID clientId = this.requireClientId(request.clientId());
        Optional<VetVisit> existing = this.repository.findByPetIdAndClientId(petId, clientId);
        if (existing.isPresent()) {
            return new CreateResult(this.toDto(existing.get(), request.date()), false);
        }
        this.rules.validateLog(request);
        VetVisit visit = this.repository.save(VetVisit.done(petId, clientId, request.visitedOn(), request.clinic(), this.clock.instant()));
        this.record(ownerId, visit, request);
        return new CreateResult(this.toDto(this.repository.saveAndFlush(visit), request.date()), true);
    }

    public VetVisitDto logFromPlan(UUID ownerId, UUID petId, UUID visitId, long expectedVersion, LogVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        VetVisit visit = this.find(petId, visitId);
        this.requireStatus(visit, VisitStatus.PLANNED, expectedVersion);
        this.rules.validateLog(request);
        this.record(ownerId, visit, request);
        return this.toDto(this.repository.saveAndFlush(visit), request.date());
    }

    public VetVisitDto update(UUID ownerId, UUID petId, UUID visitId, long expectedVersion, LogVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        VetVisit visit = this.find(petId, visitId);
        this.requireStatus(visit, VisitStatus.DONE, expectedVersion);
        this.rules.validateLog(request);
        if (request.justCheck() && visit.hasLinkedTreatment()) {
            throw new InvalidVetVisit("justCheck", InvalidVetVisit.INVALID);
        }
        this.record(ownerId, visit, request);
        return this.toDto(this.repository.saveAndFlush(visit), request.date());
    }

    public void delete(UUID ownerId, UUID petId, UUID visitId, boolean removeLinked, LocalDate date, Integer minuteOfDay) {
        LocalDate today = date == null ? LocalDate.now(this.clock) : date;
        this.clientDateValidator.validate(today);
        this.petService.getOwnedPetProfile(ownerId, petId);
        this.repository.findByIdAndPetId(visitId, petId).ifPresent(visit -> {
            if (removeLinked && !visit.isPlanned()) {
                this.removeLinked(ownerId, visit, today, minuteOfDay == null ? minuteNow() : minuteOfDay);
            }
            if (visit.getDayEventId() != null) {
                this.dayEventApi.cancel(petId, visit.getDayEventId());
            }
            this.repository.delete(visit);
        });
    }

    private void record(UUID ownerId, VetVisit visit, LogVisitRequest request) {
        long version = visit.getVersion();
        visit.record(request.visitedOn(), request.clinic(), request.note(), request.justCheck(), this.clock.instant());
        this.linkWhatHappened(ownerId, visit, request, version);
        visit.replaceTests(request.tests().stream().collect(Collectors.toMap(
                LogVisitRequest.TestRequest::kind, LogVisitRequest.TestRequest::waitingForResults, (first, second) -> first,
                () -> new EnumMap<>(VisitTestKind.class))));
        visit.replaceProcedure(request.procedure() == null ? null
                : new VisitProcedure(request.procedure().kind(), blankToNull(request.procedure().note())));
        boolean hasFollowUp = this.syncFollowUp(visit, request, version);
        visit.setOutcomes(this.outcomeRule.derive(visit.isJustCheck(), hasTreatment(visit), !visit.getTests().isEmpty(), hasFollowUp));
    }

    private void linkWhatHappened(UUID ownerId, VetVisit visit, LogVisitRequest request, long version) {
        UUID petId = visit.getPetId();
        LocalDate on = request.visitedOn();
        int minute = request.visitMinute() == null ? DEFAULT_VISIT_MINUTE : request.visitMinute();

        Set<String> linkedCodes = this.linkedCareCodes(visit);
        request.careItemCodes().stream()
                .distinct()
                .filter(code -> !linkedCodes.contains(code))
                .forEach(code -> visit.getCareEventIds().add(this.careEventId(ownerId, petId, code, on, request.date())));

        List<LogVisitRequest.ClinicMedicine> medicines = request.clinicMedicines();
        for (int index = 0; index < medicines.size(); index++) {
            UUID clientId = VisitChildIds.of(visit.getClientId(), "medicine-" + version, index);
            UUID logId = this.petLogRecorder.logMedicine(ownerId, petId, clientId, medicines.get(index).label(),
                    blankToNull(medicines.get(index).dose()), on, minute, request.date());
            visit.getLogs().put(logId, VisitLogRole.CLINIC_MEDICINE);
        }

        if (request.weightGrams() != null) {
            if (visit.hasLog(VisitLogRole.WEIGHT)) {
                throw new InvalidVetVisit("weightGrams", InvalidVetVisit.ALREADY_LINKED);
            }
            UUID clientId = VisitChildIds.of(visit.getClientId(), "weight-" + version, 0);
            visit.getLogs().put(this.petLogRecorder.logWeight(ownerId, petId, clientId, request.weightGrams(), on, minute, request.date()),
                    VisitLogRole.WEIGHT);
        }

        request.homeCourses().forEach(course ->
                visit.getCourseIds().add(this.medicationApi.start(ownerId, petId, course.toCreateRequest(request.date()))));
    }

    private Set<String> linkedCareCodes(VetVisit visit) {
        return this.careApi.events(visit.getPetId(), visit.getCareEventIds()).stream()
                .map(CareEventDto::itemCode)
                .collect(Collectors.toSet());
    }

    private UUID careEventId(UUID ownerId, UUID petId, String code, LocalDate on, LocalDate date) {
        return this.careApi.findEvent(petId, code, on)
                .orElseGet(() -> this.careApi.log(ownerId, petId, code, on, date))
                .id();
    }

    /** Returns whether the visit has a follow-up plan or visit once this change is applied. */
    private boolean syncFollowUp(VetVisit visit, LogVisitRequest request, long version) {
        LogVisitRequest.FollowUp followUp = request.followUp();
        Optional<VetVisit> existing = this.repository.findFirstByFollowUpOf(visit.getId());
        if (existing.isPresent() && !existing.get().isPlanned()) {
            return true;
        }
        if (followUp == null) {
            existing.ifPresent(plan -> this.removePlan(visit.getPetId(), plan));
            return false;
        }
        if (existing.isPresent()) {
            this.reschedule(existing.get(), followUp, request.date());
            return true;
        }
        this.createFollowUp(visit, followUp, request.date(), version);
        return true;
    }

    private void createFollowUp(VetVisit visit, LogVisitRequest.FollowUp followUp, LocalDate date, long version) {
        VetVisit plan = VetVisit.planned(visit.getPetId(), VisitChildIds.of(visit.getClientId(), "follow-up-" + version, 0),
                followUp.on(), followUp.minute(), visit.getClinic(), null, this.clock.instant());
        plan.setFollowUpOf(visit.getId());
        plan.setDayEventId(this.dayEventApi.scheduleVet(visit.getPetId(), followUp.on(), followUp.minute(), date));
        this.repository.save(plan);
    }

    private void reschedule(VetVisit plan, LogVisitRequest.FollowUp followUp, LocalDate date) {
        plan.replan(followUp.on(), followUp.minute(), plan.getClinic(), plan.getPrepNote(), this.clock.instant());
        this.syncEvent(plan.getPetId(), plan, followUp.on(), followUp.minute(), date);
    }

    private void removePlan(UUID petId, VetVisit plan) {
        if (plan.getDayEventId() != null) {
            this.dayEventApi.cancel(petId, plan.getDayEventId());
        }
        this.repository.delete(plan);
    }

    private void removeLinked(UUID ownerId, VetVisit visit, LocalDate date, int minuteOfDay) {
        UUID petId = visit.getPetId();
        this.careApi.events(petId, visit.getCareEventIds())
                .forEach(event -> this.careApi.undo(ownerId, petId, event.id(), date));
        this.petLogApi.snapshots(petId, visit.getLogs().keySet())
                .forEach(log -> this.petLogRecorder.delete(ownerId, petId, log.logId(), date));
        visit.getCourseIds().forEach(courseId -> this.medicationApi.stopOrDelete(ownerId, petId, courseId, date, minuteOfDay));
    }

    private void syncEvent(UUID petId, VetVisit visit, LocalDate on, int minute, LocalDate date) {
        if (visit.getDayEventId() == null) {
            visit.setDayEventId(this.dayEventApi.scheduleVet(petId, on, minute, date));
            return;
        }
        this.dayEventApi.rescheduleVet(petId, visit.getDayEventId(), on, minute, date);
    }

    private VetVisit find(UUID petId, UUID visitId) {
        return this.repository.findByIdAndPetId(visitId, petId).orElseThrow(VetVisitNotFound::new);
    }

    private UUID requireClientId(UUID clientId) {
        if (clientId == null) {
            throw new InvalidVetVisit("clientId", InvalidVetVisit.REQUIRED);
        }
        return clientId;
    }

    private void requireStatus(VetVisit visit, VisitStatus status, long expectedVersion) {
        if (visit.getStatus() != status) {
            throw new VisitStatusConflict();
        }
        if (visit.getVersion() != expectedVersion) {
            throw new VersionConflict();
        }
    }

    private VetVisitDto toDto(VetVisit visit, LocalDate date) {
        DayEventApi.VetAppointment appointment = visit.getDayEventId() == null ? null
                : this.dayEventApi.vetAppointments(visit.getPetId(), List.of(visit.getDayEventId())).get(visit.getDayEventId());
        UUID followUpPlanId = this.repository.findFirstByFollowUpOf(visit.getId()).map(VetVisit::getId).orElse(null);
        VisitLinks links = this.linksReader.read(visit.getPetId(), List.of(visit), date).get(visit.getId());
        return VetVisitDto.of(visit, appointment, links, followUpPlanId);
    }

    private int minuteNow() {
        LocalDateTime now = LocalDateTime.now(this.clock);
        return now.getHour() * 60 + now.getMinute();
    }

    private static boolean hasTreatment(VetVisit visit) {
        return visit.hasLinkedTreatment() || !visit.getProcedures().isEmpty();
    }

    private static PlannedAppointment planned(VetVisit plan, DayEventApi.VetAppointment appointment) {
        return new PlannedAppointment(plan.getId(), appointment.eventId(), appointment.on(), appointment.startMinute());
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
