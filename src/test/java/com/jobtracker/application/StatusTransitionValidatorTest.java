package com.jobtracker.application;

import com.jobtracker.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusTransitionValidatorTest {

    private StatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StatusTransitionValidator();
    }

    @Test
    void validate_allLegalTransitions_doNotThrow() {
        assertDoesNotThrow(() -> validator.validate(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW));
        assertDoesNotThrow(() -> validator.validate(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED));
        assertDoesNotThrow(() -> validator.validate(ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER));
        assertDoesNotThrow(() -> validator.validate(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED));

        for (ApplicationStatus currentStatus : ApplicationStatus.values()) {
            assertDoesNotThrow(() -> validator.validate(currentStatus, ApplicationStatus.WITHDRAWN));
        }
    }

    @Test
    void validate_allIllegalTransitions_throwBadRequest() {
        for (ApplicationStatus currentStatus : ApplicationStatus.values()) {
            for (ApplicationStatus newStatus : ApplicationStatus.values()) {
                if (isLegal(currentStatus, newStatus)) {
                    continue;
                }

                AppException exception = assertThrows(AppException.class,
                        () -> validator.validate(currentStatus, newStatus));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
                assertEquals(
                        "Illegal status transition from %s to %s. Allowed transitions: APPLIED->INTERVIEW, APPLIED->REJECTED, "
                                .formatted(currentStatus, newStatus)
                                + "INTERVIEW->OFFER, INTERVIEW->REJECTED, any->WITHDRAWN.",
                        exception.getMessage()
                );
            }
        }
    }

    private boolean isLegal(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        if (newStatus == ApplicationStatus.WITHDRAWN) {
            return true;
        }

        return switch (currentStatus) {
            case APPLIED -> newStatus == ApplicationStatus.INTERVIEW || newStatus == ApplicationStatus.REJECTED;
            case INTERVIEW -> newStatus == ApplicationStatus.OFFER || newStatus == ApplicationStatus.REJECTED;
            default -> false;
        };
    }
}
