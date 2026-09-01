package att.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

import att.exceptions.BadRequestException;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

public class AttUtility {

    public static void validateOpenCloseDateInput(Supplier<OffsetDateTime> openCloseDateTime, LocalDate workDate,
                                                  String errorMsg) {
        if (isNull(openCloseDateTime.get()) || isFalse(openCloseDateTime.get().toLocalDate().equals(workDate))) {
            throw new BadRequestException(errorMsg);
        }
    }
}
