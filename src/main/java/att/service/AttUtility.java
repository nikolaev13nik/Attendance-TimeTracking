package att.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

import att.exceptions.BadRequestException;

import static att.exceptions.ErrorConstants.FUTURE_WORK_DATE_MSG;
import static att.exceptions.ErrorConstants.PREVIOUS_DAY_REQUIRES_ADMIN_MSG;
import static att.exceptions.ErrorProvider.workDateMismatch;
import static att.security.JwtClaimsResolver.isAdministrator;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

public class AttUtility {

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
}
