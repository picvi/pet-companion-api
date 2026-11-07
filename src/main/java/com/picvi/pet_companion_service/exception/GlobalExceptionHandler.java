package com.picvi.pet_companion_service.exception;

import com.picvi.pet_companion_service.dto.EmailNotVerifiedResponse;
import com.picvi.pet_companion_service.dto.ErrorResponse;
import com.picvi.pet_companion_service.exception.account.CurrentPasswordIncorrect;
import com.picvi.pet_companion_service.exception.account.PasswordUnchanged;
import com.picvi.pet_companion_service.exception.breed.BreedNotFound;
import com.picvi.pet_companion_service.exception.care.CareEventNotFound;
import com.picvi.pet_companion_service.exception.care.CareItemNotFound;
import com.picvi.pet_companion_service.exception.care.IntervalNotEditable;
import com.picvi.pet_companion_service.exception.care.InvalidEventDate;
import com.picvi.pet_companion_service.exception.care.InvalidInterval;
import com.picvi.pet_companion_service.exception.development.ActivityAlreadyCompleted;
import com.picvi.pet_companion_service.exception.development.ActivityNotFound;
import com.picvi.pet_companion_service.exception.development.AloneActivityNeedsSessions;
import com.picvi.pet_companion_service.exception.development.NotAnAloneActivity;
import com.picvi.pet_companion_service.exception.development.SessionNotFound;
import com.picvi.pet_companion_service.exception.development.InvalidOutcome;
import com.picvi.pet_companion_service.exception.rhythm.InvalidDayTimes;
import com.picvi.pet_companion_service.exception.rhythm.MealsOutOfRange;
import com.picvi.pet_companion_service.exception.rhythm.MealOutsideDay;
import com.picvi.pet_companion_service.exception.rhythm.AwakeOutOfRange;
import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;
import com.picvi.pet_companion_service.exception.petlog.InvalidLog;
import com.picvi.pet_companion_service.exception.petlog.LogTimeInFuture;
import com.picvi.pet_companion_service.exception.petlog.LogNotFound;
import com.picvi.pet_companion_service.exception.dayevent.InvalidDayEvent;
import com.picvi.pet_companion_service.exception.dayevent.DayEventNotFound;
import com.picvi.pet_companion_service.exception.condition.ConditionNotFound;
import com.picvi.pet_companion_service.exception.condition.InvalidCondition;
import com.picvi.pet_companion_service.exception.nutrition.FoodNotTriable;
import com.picvi.pet_companion_service.exception.nutrition.InvalidTriedFood;
import com.picvi.pet_companion_service.exception.nutrition.SafetyFoodNotFound;
import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.exception.medical.VetVisitNotFound;
import com.picvi.pet_companion_service.exception.brief.InvalidBriefRequest;
import com.picvi.pet_companion_service.dto.AllergyMatchResponse;
import com.picvi.pet_companion_service.exception.nutrition.AllergyMatchRequired;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeAlreadyActive;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeNotActive;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeNotFound;
import com.picvi.pet_companion_service.exception.nutrition.FoodNotActive;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodChange;
import com.picvi.pet_companion_service.exception.nutrition.FoodProductNotFound;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodProduct;
import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;
import com.picvi.pet_companion_service.exception.nutrition.PetFoodNotFound;
import com.picvi.pet_companion_service.exception.nutrition.MainFoodLimit;
import com.picvi.pet_companion_service.exception.nutrition.SecondMainFood;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.exception.nutrition.ProteinRequired;
import com.picvi.pet_companion_service.exception.pet.InvalidBirthDate;
import com.picvi.pet_companion_service.exception.pet.InvalidMicrochip;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.exception.triage.InvalidCheck;
import com.picvi.pet_companion_service.exception.triage.CheckConflict;
import com.picvi.pet_companion_service.lostdog.exception.InvalidLostReport;
import com.picvi.pet_companion_service.lostdog.exception.LostReportNotFound;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCheck.class)
    public ResponseEntity<ErrorResponse> handleInvalidCheck(InvalidCheck ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(CheckConflict.class)
    public ResponseEntity<ErrorResponse> handleCheckConflict(CheckConflict ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(UserDoesNotExist.class)
    public ResponseEntity<ErrorResponse> handleUserDoesNotExist(UserDoesNotExist ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(PasswordIncorrect.class)
    public ResponseEntity<ErrorResponse> handlePasswordIncorrect(PasswordIncorrect ex) {
        return error(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(EmailNotVerified.class)
    public ResponseEntity<EmailNotVerifiedResponse> handleEmailNotVerified(EmailNotVerified ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new EmailNotVerifiedResponse(HttpStatus.FORBIDDEN.value(), ex.getCode(), ex.getMessage(), ex.getEmail()));
    }

    @ExceptionHandler(InvalidVerificationCode.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationCode(InvalidVerificationCode ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidRefreshToken.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshToken ex) {
        return error(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(PetNotFound.class)
    public ResponseEntity<ErrorResponse> handlePetNotFound(PetNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidBirthDate.class)
    public ResponseEntity<ErrorResponse> handleInvalidBirthDate(InvalidBirthDate ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidMicrochip.class)
    public ResponseEntity<ErrorResponse> handleInvalidMicrochip(InvalidMicrochip ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidLostReport.class)
    public ResponseEntity<ErrorResponse> handleInvalidLostReport(InvalidLostReport ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(LostReportNotFound.class)
    public ResponseEntity<ErrorResponse> handleLostReportNotFound(LostReportNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(CurrentPasswordIncorrect.class)
    public ResponseEntity<ErrorResponse> handleCurrentPasswordIncorrect(CurrentPasswordIncorrect ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(PasswordUnchanged.class)
    public ResponseEntity<ErrorResponse> handlePasswordUnchanged(PasswordUnchanged ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(BreedNotFound.class)
    public ResponseEntity<ErrorResponse> handleBreedNotFound(BreedNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ActivityNotFound.class)
    public ResponseEntity<ErrorResponse> handleActivityNotFound(ActivityNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidOutcome.class)
    public ResponseEntity<ErrorResponse> handleInvalidOutcome(InvalidOutcome ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ActivityAlreadyCompleted.class)
    public ResponseEntity<ErrorResponse> handleActivityAlreadyCompleted(ActivityAlreadyCompleted ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(InvalidDate.class)
    public ResponseEntity<ErrorResponse> handleInvalidDate(InvalidDate ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(CareItemNotFound.class)
    public ResponseEntity<ErrorResponse> handleCareItemNotFound(CareItemNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(CareEventNotFound.class)
    public ResponseEntity<ErrorResponse> handleCareEventNotFound(CareEventNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidEventDate.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventDate(InvalidEventDate ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidInterval.class)
    public ResponseEntity<ErrorResponse> handleInvalidInterval(InvalidInterval ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(IntervalNotEditable.class)
    public ResponseEntity<ErrorResponse> handleIntervalNotEditable(IntervalNotEditable ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidDayTimes.class)
    public ResponseEntity<ErrorResponse> handleInvalidDayTimes(InvalidDayTimes ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MealsOutOfRange.class)
    public ResponseEntity<ErrorResponse> handleMealsOutOfRange(MealsOutOfRange ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MealOutsideDay.class)
    public ResponseEntity<ErrorResponse> handleMealOutsideDay(MealOutsideDay ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(AwakeOutOfRange.class)
    public ResponseEntity<ErrorResponse> handleAwakeOutOfRange(AwakeOutOfRange ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(AloneActivityNeedsSessions.class)
    public ResponseEntity<ErrorResponse> handleAloneActivityNeedsSessions(AloneActivityNeedsSessions ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(NotAnAloneActivity.class)
    public ResponseEntity<ErrorResponse> handleNotAnAloneActivity(NotAnAloneActivity ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(SessionNotFound.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidLog.class)
    public ResponseEntity<ErrorResponse> handleInvalidLog(InvalidLog ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidCursor.class)
    public ResponseEntity<ErrorResponse> handleInvalidCursor(InvalidCursor ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(LogTimeInFuture.class)
    public ResponseEntity<ErrorResponse> handleLogTimeInFuture(LogTimeInFuture ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(LogNotFound.class)
    public ResponseEntity<ErrorResponse> handleLogNotFound(LogNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidDayEvent.class)
    public ResponseEntity<ErrorResponse> handleInvalidDayEvent(InvalidDayEvent ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(DayEventNotFound.class)
    public ResponseEntity<ErrorResponse> handleDayEventNotFound(DayEventNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ProteinRequired.class)
    public ResponseEntity<ErrorResponse> handleProteinRequired(ProteinRequired ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidFoodProduct.class)
    public ResponseEntity<ErrorResponse> handleInvalidFoodProduct(InvalidFoodProduct ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(FoodProductNotFound.class)
    public ResponseEntity<ErrorResponse> handleFoodProductNotFound(FoodProductNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(PetFoodNotFound.class)
    public ResponseEntity<ErrorResponse> handlePetFoodNotFound(PetFoodNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidPetFood.class)
    public ResponseEntity<ErrorResponse> handleInvalidPetFood(InvalidPetFood ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MainFoodLimit.class)
    public ResponseEntity<ErrorResponse> handleMainFoodLimit(MainFoodLimit ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(SecondMainFood.class)
    public ResponseEntity<ErrorResponse> handleSecondMainFood(SecondMainFood ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(VersionConflict.class)
    public ResponseEntity<ErrorResponse> handleVersionConflict(VersionConflict ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(FoodNotActive.class)
    public ResponseEntity<ErrorResponse> handleFoodNotActive(FoodNotActive ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ConditionNotFound.class)
    public ResponseEntity<ErrorResponse> handleConditionNotFound(ConditionNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidCondition.class)
    public ResponseEntity<ErrorResponse> handleInvalidCondition(InvalidCondition ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(SafetyFoodNotFound.class)
    public ResponseEntity<ErrorResponse> handleSafetyFoodNotFound(SafetyFoodNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(FoodNotTriable.class)
    public ResponseEntity<ErrorResponse> handleFoodNotTriable(FoodNotTriable ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(InvalidTriedFood.class)
    public ResponseEntity<ErrorResponse> handleInvalidTriedFood(InvalidTriedFood ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(VetVisitNotFound.class)
    public ResponseEntity<ErrorResponse> handleVetVisitNotFound(VetVisitNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidVetVisit.class)
    public ResponseEntity<ErrorResponse> handleInvalidVetVisit(InvalidVetVisit ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(InvalidBriefRequest.class)
    public ResponseEntity<ErrorResponse> handleInvalidBriefRequest(InvalidBriefRequest ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(AllergyMatchRequired.class)
    public ResponseEntity<AllergyMatchResponse> handleAllergyMatchRequired(AllergyMatchRequired ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new AllergyMatchResponse(HttpStatus.CONFLICT.value(), ex.getCode(), ex.getMessage(), ex.getMatches()));
    }

    @ExceptionHandler(FoodChangeAlreadyActive.class)
    public ResponseEntity<ErrorResponse> handleFoodChangeAlreadyActive(FoodChangeAlreadyActive ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(FoodChangeNotActive.class)
    public ResponseEntity<ErrorResponse> handleFoodChangeNotActive(FoodChangeNotActive ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(FoodChangeNotFound.class)
    public ResponseEntity<ErrorResponse> handleFoodChangeNotFound(FoodChangeNotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidFoodChange.class)
    public ResponseEntity<ErrorResponse> handleInvalidFoodChange(InvalidFoodChange ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "VALIDATION_FAILED", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
        return badRequest("MALFORMED_REQUEST", "The request body could not be read");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException ex) {
        return badRequest("INVALID_PARAMETER", ex.getName() + " has an invalid value");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return badRequest("MISSING_PARAMETER", ex.getParameterName() + " is required");
    }

    private ResponseEntity<ErrorResponse> badRequest(String code, String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, code, message));
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, ApiException ex) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), ex.getCode(), ex.getMessage()));
    }
}
