package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.exception.medical.VetVisitNotFound;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.medical.domain.BookedAppointment;
import com.picvi.pet_companion_service.medical.domain.VetAppointmentRule;
import com.picvi.pet_companion_service.medical.domain.VetVisitRules;
import com.picvi.pet_companion_service.medical.domain.VisitDay;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.dto.CreateVetVisitRequest;
import com.picvi.pet_companion_service.medical.dto.NextVetAppointmentDto;
import com.picvi.pet_companion_service.medical.dto.UpdateVetVisitRequest;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.dto.VetVisitListResponse;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.EnumSet;
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
    public VetVisitListResponse list(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        List<VetVisit> visits = this.repository.findAllByPetIdOrderByVisitedOnDescCreatedAtDesc(petId);
        List<DayEventApi.VetAppointment> fromDay = this.dayEventApi.vetAppointmentsFrom(petId,
                date.minusDays(VetAppointmentRule.PASSED_VISIBLE_DAYS));
        Map<UUID, DayEventApi.VetAppointment> linked = this.dayEventApi.vetAppointments(petId, visits.stream()
                .map(VetVisit::getDayEventId)
                .filter(Objects::nonNull)
                .toList());
        Map<UUID, UUID> bookedBy = visits.stream()
                .filter(visit -> visit.getDayEventId() != null)
                .collect(Collectors.toMap(VetVisit::getDayEventId, VetVisit::getId));
        List<BookedAppointment> booked = fromDay.stream()
                .map(appointment -> new BookedAppointment(appointment.eventId(), appointment.on(), appointment.startMinute(),
                        bookedBy.get(appointment.eventId())))
                .toList();
        List<VisitDay> days = visits.stream().map(visit -> new VisitDay(visit.getId(), visit.getVisitedOn())).toList();
        NextVetAppointmentDto next = this.appointmentRule.evaluate(booked, days, date).map(NextVetAppointmentDto::of).orElse(null);
        return new VetVisitListResponse(date,
                visits.stream().map(visit -> VetVisitDto.of(visit, linkedAppointment(linked, visit))).toList(), next);
    }

    @Transactional(readOnly = true)
    public VetVisitDto get(UUID ownerId, UUID petId, UUID visitId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        VetVisit visit = this.repository.findByIdAndPetId(visitId, petId).orElseThrow(VetVisitNotFound::new);
        return VetVisitDto.of(visit, this.appointmentOf(petId, visit));
    }

    public CreateResult create(UUID ownerId, UUID petId, CreateVetVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        PetProfile pet = this.petService.getOwnedPetProfile(ownerId, petId);
        Optional<VetVisit> existing = this.repository.findByPetIdAndClientId(petId, request.clientId());
        if (existing.isPresent()) {
            return new CreateResult(VetVisitDto.of(existing.get(), this.appointmentOf(petId, existing.get())), false);
        }
        String clinic = blankToNull(request.clinic());
        String note = blankToNull(request.note());
        this.rules.validate(request.visitedOn(), request.nextCheckOn(), request.nextCheckMinute(), request.outcomes(), clinic, note,
                pet.birthDate(), request.date());
        VetVisit visit = new VetVisit(petId, request.clientId(), request.visitedOn(),
                outcomeSet(request.outcomes()), clinic, note, request.briefHelpful(), this.clock.instant());
        if (request.nextCheckOn() != null) {
            visit.setDayEventId(
                    this.dayEventApi.scheduleVet(petId, request.nextCheckOn(), request.nextCheckMinute(), request.date()));
        }
        VetVisit saved = this.repository.saveAndFlush(visit);
        return new CreateResult(VetVisitDto.of(saved, this.appointmentOf(petId, saved)), true);
    }

    public VetVisitDto update(UUID ownerId, UUID petId, UUID visitId, long expectedVersion, UpdateVetVisitRequest request) {
        this.clientDateValidator.validate(request.date());
        PetProfile pet = this.petService.getOwnedPetProfile(ownerId, petId);
        VetVisit visit = this.repository.findByIdAndPetId(visitId, petId).orElseThrow(VetVisitNotFound::new);
        if (visit.getVersion() != expectedVersion) {
            throw new VersionConflict();
        }
        DayEventApi.VetAppointment current = this.appointmentOf(petId, visit);
        boolean unchanged = current != null
                && current.on().equals(request.nextCheckOn())
                && Integer.valueOf(current.startMinute()).equals(request.nextCheckMinute());
        String clinic = blankToNull(request.clinic());
        String note = blankToNull(request.note());
        this.rules.validate(request.visitedOn(), unchanged ? null : request.nextCheckOn(),
                unchanged ? null : request.nextCheckMinute(), request.outcomes(), clinic, note, pet.birthDate(), request.date());
        visit.replace(request.visitedOn(), outcomeSet(request.outcomes()), clinic, note, request.briefHelpful(),
                this.clock.instant());
        this.syncAppointment(petId, visit, current, unchanged, request);
        VetVisit saved = this.repository.saveAndFlush(visit);
        return VetVisitDto.of(saved, this.appointmentOf(petId, saved));
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

    private void syncAppointment(UUID petId, VetVisit visit, DayEventApi.VetAppointment current, boolean unchanged,
                                 UpdateVetVisitRequest request) {
        if (request.nextCheckOn() == null) {
            if (visit.getDayEventId() != null) {
                this.dayEventApi.cancel(petId, visit.getDayEventId());
            }
            visit.setDayEventId(null);
        } else if (current == null) {
            visit.setDayEventId(
                    this.dayEventApi.scheduleVet(petId, request.nextCheckOn(), request.nextCheckMinute(), request.date()));
        } else if (!unchanged) {
            this.dayEventApi.rescheduleVet(petId, current.eventId(), request.nextCheckOn(), request.nextCheckMinute(), request.date());
        }
    }

    private DayEventApi.VetAppointment appointmentOf(UUID petId, VetVisit visit) {
        UUID eventId = visit.getDayEventId();
        if (eventId == null) {
            return null;
        }
        return this.dayEventApi.vetAppointments(petId, List.of(eventId)).get(eventId);
    }

    private static DayEventApi.VetAppointment linkedAppointment(Map<UUID, DayEventApi.VetAppointment> linked, VetVisit visit) {
        return visit.getDayEventId() == null ? null : linked.get(visit.getDayEventId());
    }

    private static Set<VisitOutcome> outcomeSet(List<VisitOutcome> outcomes) {
        Set<VisitOutcome> set = EnumSet.noneOf(VisitOutcome.class);
        set.addAll(outcomes);
        return set;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
