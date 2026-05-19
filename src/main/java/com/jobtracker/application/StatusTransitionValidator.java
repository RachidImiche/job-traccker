package com.jobtracker.application;

import com.jobtracker.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StatusTransitionValidator {

    public void validate(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        if (newStatus == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        if (isAllowed(currentStatus, newStatus)) {
            return;
        }

        throw new AppException(
                HttpStatus.BAD_REQUEST,
                "Illegal status transition from %s to %s. "
                        .formatted(currentStatus, newStatus)
                        + "Allowed transitions: APPLIED->INTERVIEW, APPLIED->REJECTED, "
                        + "INTERVIEW->OFFER, INTERVIEW->REJECTED, any->WITHDRAWN."
        );
    }

    private boolean isAllowed(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        if (newStatus == ApplicationStatus.WITHDRAWN) {
            return true;
        }

        if (currentStatus == null) {
            return false;
        }

        return switch (currentStatus) {
            case APPLIED -> newStatus == ApplicationStatus.INTERVIEW || newStatus == ApplicationStatus.REJECTED;
            case INTERVIEW -> newStatus == ApplicationStatus.OFFER || newStatus == ApplicationStatus.REJECTED;
            default -> false;
        };
    }
}
