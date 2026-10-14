package att.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

import att.exceptions.BadRequestException;

import static att.exceptions.ErrorConstants.FUTURE_WORK_DATE_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG;
import static att.exceptions.ErrorConstants.PREVIOUS_DAY_REQUIRES_ADMIN_MSG;
import static att.exceptions.ErrorProvider.leaveDayAmountExceedsFullDay;
import static att.exceptions.ErrorProvider.workDateMismatch;
import static att.security.JwtClaimsResolver.isAdministrator;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

public class AttUtility {

    public static final double FULL_DAY = 1.0;

    public static void validateOpenCloseDateInput(Supplier<OffsetDateTime> openCloseDateTime, LocalDate workDate,
                                                  String errorMsg) {
        OffsetDateTime moment = openCloseDateTime.get();
        if (isNull(moment) || isFalse(moment.toLocalDate().equals(workDate))) {
            throw new BadRequestException(errorMsg);
        }
        LocalDate callerToday = OffsetDateTime.now(moment.getOffset()).toLocalDate();
        if (workDate.isAfter(callerToday)) {
            workDateMismatch(String.format(FUTURE_WORK_DATE_MSG, workDate, callerToday));
        }
        if (workDate.isBefore(callerToday) && isFalse(isAdministrator())) {
            workDateMismatch(String.format(PREVIOUS_DAY_REQUIRES_ADMIN_MSG, workDate, callerToday));
        }
    }

    public static double amountOrZero(Double amount) {
        return isNull(amount) ? 0.0 : amount;
    }

    public static Double nullIfZero(Double amount) {
        return isNull(amount) || amount == 0.0 ? null : amount;
    }

    public static void validateLeaveDayAmounts(LocalDate leaveDate, Double vacationAmount, Double sickAmount) {
        if (amountOrZero(vacationAmount) + amountOrZero(sickAmount) > FULL_DAY) {
            leaveDayAmountExceedsFullDay(
                    String.format(LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG, leaveDate, vacationAmount, sickAmount));
        }
    }
}
