package com.picvi.pet_companion_service.lostdog.service;

import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.lostdog.domain.ChipRegistry;
import com.picvi.pet_companion_service.lostdog.domain.LostStatus;
import com.picvi.pet_companion_service.lostdog.domain.LostStepCode;
import com.picvi.pet_companion_service.lostdog.dto.*;
import com.picvi.pet_companion_service.lostdog.entity.LostReport;
import com.picvi.pet_companion_service.lostdog.entity.LostReportStep;
import com.picvi.pet_companion_service.lostdog.entity.PetLostProfile;
import com.picvi.pet_companion_service.lostdog.exception.InvalidLostReport;
import com.picvi.pet_companion_service.lostdog.exception.LostReportNotFound;
import com.picvi.pet_companion_service.lostdog.repository.LostReportRepository;
import com.picvi.pet_companion_service.lostdog.repository.LostReportStepRepository;
import com.picvi.pet_companion_service.lostdog.repository.PetLostProfileRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.timeline.domain.LostTimelineRow;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LostDogService {
    private final PetService petService;
    private final PetLostProfileRepository profileRepository;
    private final LostReportRepository reportRepository;
    private final LostReportStepRepository stepRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public LostProfileResponse profile(UUID ownerId, UUID petId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        return this.profileRepository.findById(petId).map(this::profileDto)
                .orElse(new LostProfileResponse(null, null, null, null));
    }

    public LostProfileResponse saveProfile(UUID ownerId, UUID petId, SaveLostProfileRequest request) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        ChipRegistry registry = request.chipRegistry();
        String registryName = blankToNull(request.chipRegistryName());
        if (registry == ChipRegistry.OTHER && registryName == null) {
            throw new InvalidLostReport("Enter the chip registry name");
        }
        if (registry != ChipRegistry.OTHER && registryName != null) {
            throw new InvalidLostReport("A registry name is allowed only when Other is selected");
        }
        Instant now = this.clock.instant();
        PetLostProfile profile = this.profileRepository.findById(petId).orElseGet(() -> new PetLostProfile(petId, now));
        profile.setChipRegistry(registry);
        profile.setChipRegistryName(registryName);
        profile.setMarkings(blankToNull(request.markings()));
        profile.touch(now);
        return this.profileDto(this.profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public Optional<LostReportDto> active(UUID ownerId, UUID petId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        return this.reportRepository.findFirstByPetIdAndStatus(petId, LostStatus.ACTIVE).map(this::dto);
    }

    @Transactional(readOnly = true)
    public List<LostTimelineRow> timeline(UUID petId, LocalDate throughDate) {
        Instant through = throughDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = this.clock.instant();
        return this.reportRepository.findAllByPetIdAndStartedAtLessThanEqualOrderByStartedAtDesc(petId, through).stream()
                .map(report -> {
                    Instant end = report.getEndedAt() == null ? now : report.getEndedAt();
                    return new LostTimelineRow(report.getId(), LocalDateTime.ofInstant(report.getStartedAt(), ZoneOffset.UTC),
                            report.getEndedAt(), report.getStatus(), Math.max(0, Duration.between(report.getStartedAt(), end).getSeconds()),
                            report.getLastSeenPlace());
                }).toList();
    }

    public CreateResult start(UUID ownerId, UUID petId, StartLostReportRequest request) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        Instant now = this.clock.instant();
        validateLastSeen(request.lastSeenAt(), now);
        validateCoordinates(request.lastSeenLat(), request.lastSeenLng());
        String place = request.lastSeenPlace().trim();
        if (place.isEmpty()) throw new InvalidLostReport("Enter where the dog was last seen");

        Optional<LostReport> clientRetry = this.reportRepository.findByPetIdAndClientId(petId, request.clientId());
        if (clientRetry.isPresent()) return new CreateResult(this.dto(clientRetry.get()), false);
        Optional<LostReport> alreadyActive = this.reportRepository.findFirstByPetIdAndStatus(petId, LostStatus.ACTIVE);
        if (alreadyActive.isPresent()) return new CreateResult(this.dto(alreadyActive.get()), false);

        LostReport report = new LostReport(petId, request.clientId(), now, safeLastSeen(request.lastSeenAt(), now), place,
                request.lastSeenLat(), request.lastSeenLng(), request.posterPhone().trim());
        try {
            return new CreateResult(this.dto(this.reportRepository.saveAndFlush(report)), true);
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidLostReport("A lost report is already active for this pet");
        }
    }

    public LostReportDto updateLastSeen(UUID ownerId, UUID petId, UUID reportId, long expectedVersion,
                                        UpdateLostSeenRequest request) {
        LostReport report = this.editableReport(ownerId, petId, reportId, expectedVersion);
        Instant now = this.clock.instant();
        validateLastSeen(request.lastSeenAt(), now);
        validateCoordinates(request.lastSeenLat(), request.lastSeenLng());
        String place = request.lastSeenPlace().trim();
        if (place.isEmpty()) throw new InvalidLostReport("Enter where the dog was last seen");
        report.updateLastSeen(safeLastSeen(request.lastSeenAt(), now), place, request.lastSeenLat(), request.lastSeenLng(),
                blankToNull(request.posterPhone()), blankToNull(request.posterMessage()), now);
        return this.dto(this.reportRepository.saveAndFlush(report));
    }

    public LostReportDto setStep(UUID ownerId, UUID petId, UUID reportId, long expectedVersion,
                                 LostStepCode code, boolean done) {
        LostReport report = this.editableReport(ownerId, petId, reportId, expectedVersion);
        Instant now = this.clock.instant();
        LostReportStep step = this.stepRepository.findById(new com.picvi.pet_companion_service.lostdog.entity.LostReportStepId(reportId, code))
                .orElseGet(() -> new LostReportStep(reportId, code, null));
        step.setDone(done, now);
        this.stepRepository.save(step);
        report.touch(now);
        this.reportRepository.saveAndFlush(report);
        return this.dto(report);
    }

    public LostReportDto close(UUID ownerId, UUID petId, UUID reportId, long expectedVersion, LostStatus finalStatus) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        LostReport report = this.reportRepository.findByIdAndPetId(reportId, petId).orElseThrow(LostReportNotFound::new);
        if (report.getStatus() == finalStatus) return this.dto(report);
        if (report.getStatus() != LostStatus.ACTIVE) throw new InvalidLostReport("This report has already been closed");
        if (report.getVersion() != expectedVersion) throw new VersionConflict();
        report.close(finalStatus, this.clock.instant());
        this.stepRepository.deleteAll(this.stepRepository.findAllByIdReportId(reportId));
        return this.dto(this.reportRepository.saveAndFlush(report));
    }

    private LostReport editableReport(UUID ownerId, UUID petId, UUID reportId, long expectedVersion) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        LostReport report = this.reportRepository.findByIdAndPetId(reportId, petId).orElseThrow(LostReportNotFound::new);
        if (report.getStatus() != LostStatus.ACTIVE) throw new InvalidLostReport("This report is no longer active");
        if (report.getVersion() != expectedVersion) throw new VersionConflict();
        return report;
    }

    private LostProfileResponse profileDto(PetLostProfile profile) {
        return new LostProfileResponse(profile.getChipRegistry(), profile.getChipRegistryName(),
                profile.getMarkings(), profile.getUpdatedAt());
    }

    private LostReportDto dto(LostReport report) {
        return LostReportDto.of(report, this.stepRepository.findAllByIdReportId(report.getId()));
    }

    private static void validateLastSeen(Instant lastSeenAt, Instant now) {
        if (lastSeenAt.isAfter(now.plus(Duration.ofMinutes(5)))) {
            throw new InvalidLostReport("Last-seen time cannot be in the future");
        }
    }

    private static Instant safeLastSeen(Instant lastSeenAt, Instant now) {
        return lastSeenAt.isAfter(now) ? now : lastSeenAt;
    }

    private static void validateCoordinates(Double lat, Double lng) {
        if ((lat == null) != (lng == null)) throw new InvalidLostReport("Location needs both latitude and longitude");
        if (lat != null && (lat < -90 || lat > 90 || lng < -180 || lng > 180)) {
            throw new InvalidLostReport("Location coordinates are outside the valid range");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateResult(LostReportDto report, boolean created) {}
}
