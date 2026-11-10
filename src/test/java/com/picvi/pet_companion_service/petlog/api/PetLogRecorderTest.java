package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.NewLog;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetLogRecorderTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private PetLogService logService;

    private PetLogRecorder recorder;
    private UUID savedLogId;

    @BeforeEach
    void setUp() {
        this.recorder = new PetLogRecorder(this.logService);
        this.savedLogId = UUID.randomUUID();
    }

    @Test
    void medicineIsLoggedWithLabelAndDoseAndReturnsTheLogId() {
        givenLogIsSaved();

        UUID logId = this.recorder.logMedicine(OWNER, PET, CLIENT_ID, "Meloxicam", "0.5 ml", TODAY, 600, TODAY);

        NewLog newLog = captured();
        assertThat(logId).isEqualTo(this.savedLogId);
        assertThat(newLog.type()).isEqualTo(LogType.MEDICINE);
        assertThat(newLog.clientId()).isEqualTo(CLIENT_ID);
        assertThat(newLog.values().label()).isEqualTo("Meloxicam");
        assertThat(newLog.values().dose()).isEqualTo("0.5 ml");
        assertThat(newLog.minuteOfDay()).isEqualTo(600);
    }

    @Test
    void weightIsLoggedInGrams() {
        givenLogIsSaved();

        this.recorder.logWeight(OWNER, PET, CLIENT_ID, 8200, TODAY, 600, TODAY);

        NewLog newLog = captured();
        assertThat(newLog.type()).isEqualTo(LogType.WEIGHT);
        assertThat(newLog.values().weightGrams()).isEqualTo(8200);
    }

    @Test
    void deleteUndoesTheLog() {
        this.recorder.delete(OWNER, PET, this.savedLogId, TODAY);

        verify(this.logService).undo(OWNER, PET, this.savedLogId, TODAY);
    }

    private void givenLogIsSaved() {
        PetLogDto saved = mock(PetLogDto.class);
        when(saved.id()).thenReturn(this.savedLogId);
        when(this.logService.log(eq(OWNER), eq(PET), any(), eq(TODAY))).thenReturn(new PetLogService.LogResult(saved, true));
    }

    private NewLog captured() {
        ArgumentCaptor<NewLog> captor = ArgumentCaptor.forClass(NewLog.class);
        verify(this.logService).log(eq(OWNER), eq(PET), captor.capture(), eq(TODAY));
        return captor.getValue();
    }
}
