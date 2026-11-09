package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.exception.medical.VetVisitNotFound;
import com.picvi.pet_companion_service.exception.medical.VisitStatusConflict;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.medical.domain.PlannedAppointment;
import com.picvi.pet_companion_service.medical.domain.VetAppointmentRule;
import com.picvi.pet_companion_service.medical.domain.VetVisitRules;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.dto.LastVisitDto;
import com.picvi.pet_companion_service.medical.dto.NextVetAppointmentDto;
import com.picvi.pet_companion_service.medical.dto.PlanVisitRequest;
import com.picvi.pet_companion_service.medical.dto.UpcomingVisitsResponse;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.dto.VisitLinks;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class VetVisitService {
    public record CreateResult(VetVisitDto visit, boolean created) {
    }

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final VetVisitRepository repository;
    private final VetVisitRules rules;
    private final DayEventApi dayEventApi;
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
        return this.toDto(this.find(petId, visitId));
    }

    public CreateResult plan(UUID ownerId, UUID petId, PlanVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        if (request.clientId() == null) {
            throw new InvalidVetVisit("clientId", InvalidVetVisit.REQUIRED);
        }
        Optional<VetVisit> existing = this.repository.findByPetIdAndClientId(petId, request.clientId());
        if (existing.isPresent()) {
            return new CreateResult(this.toDto(existing.get()), false);
        }
        String clinic = blankToNull(request.clinic());
        String prepNote = blankToNull(request.prepNote());
        this.rules.validatePlan(request.scheduledOn(), request.scheduledMinute(), clinic, prepNote, request.date());
        VetVisit visit = VetVisit.planned(petId, request.clientId(), request.scheduledOn(), request.scheduledMinute(), clinic,
                prepNote, this.clock.instant());
        visit.setDayEventId(this.dayEventApi.scheduleVet(petId, request.scheduledOn(), request.scheduledMinute(), request.date()));
        return new CreateResult(this.toDto(this.repository.saveAndFlush(visit)), true);
    }

    public VetVisitDto updatePlan(UUID ownerId, UUID petId, UUID visitId, long expectedVersion, PlanVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        VetVisit visit = this.find(petId, visitId);
        this.requirePlanned(visit, expectedVersion);
        String clinic = blankToNull(request.clinic());
        String prepNote = blankToNull(request.prepNote());
        this.rules.validatePlan(request.scheduledOn(), request.scheduledMinute(), clinic, prepNote, request.date());
        visit.replan(request.scheduledOn(), request.scheduledMinute(), clinic, prepNote, this.clock.instant());
        this.syncEvent(petId, visit, request);
        return this.toDto(this.repository.saveAndFlush(visit));
    }

    public void delete(UUID ownerId, UUID petId, UUID visitId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        this.repository.findByIdAndPetId(visitId, petId).ifPresent(visit -> {
            if (visit.getDayEventId() != null) {
                this.dayEventApi.cancel(petId, visit.getDayEventId());
            }
            this.repository.delete(visit);
        });
    }

    private void syncEvent(UUID petId, VetVisit visit, PlanVisitRequest request) {
        if (visit.getDayEventId() == null) {
            visit.setDayEventId(this.dayEventApi.scheduleVet(petId, request.scheduledOn(), request.scheduledMinute(), request.date()));
            return;
        }
        this.dayEventApi.rescheduleVet(petId, visit.getDayEventId(), request.scheduledOn(), request.scheduledMinute(), request.date());
    }

    private VetVisit find(UUID petId, UUID visitId) {
        return this.repository.findByIdAndPetId(visitId, petId).orElseThrow(VetVisitNotFound::new);
    }

    private void requirePlanned(VetVisit visit, long expectedVersion) {
        if (!visit.isPlanned()) {
            throw new VisitStatusConflict();
        }
        if (visit.getVersion() != expectedVersion) {
            throw new VersionConflict();
        }
    }

    private VetVisitDto toDto(VetVisit visit) {
        DayEventApi.VetAppointment appointment = visit.getDayEventId() == null ? null
                : this.dayEventApi.vetAppointments(visit.getPetId(), List.of(visit.getDayEventId())).get(visit.getDayEventId());
        UUID followUpPlanId = this.repository.findFirstByFollowUpOf(visit.getId()).map(VetVisit::getId).orElse(null);
        return VetVisitDto.of(visit, appointment, VisitLinks.none(), followUpPlanId);
    }

    private static PlannedAppointment planned(VetVisit plan, DayEventApi.VetAppointment appointment) {
        return new PlannedAppointment(plan.getId(), appointment.eventId(), appointment.on(), appointment.startMinute());
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
