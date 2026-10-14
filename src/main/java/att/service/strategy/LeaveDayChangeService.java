package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import att.context.DataTimeContext;
import att.dto.EditLeaveDayDto;
import att.mapper.LeaveDayMapper;
import att.model.LeaveDay;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.LEAVE_DAY_DATE_ALREADY_RECORDED_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_EMPTY_MSG;
import static att.exceptions.ErrorProvider.leaveDayDateAlreadyRecorded;
import static att.exceptions.ErrorProvider.leaveDayEmpty;
import static att.service.AttUtility.amountOrZero;
import static att.service.AttUtility.nullIfZero;
import static att.service.AttUtility.validateLeaveDayAmounts;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

@Service
public class LeaveDayChangeService extends DataTimeServiceBase<EditLeaveDayDto> {

    @Autowired
    private LeaveDayMapper leaveDayMapper;

    @Override
    protected void fetch(DataTimeContext<EditLeaveDayDto> context) {
        context.putLeaveDayOnContext(findLeaveDayOrThrow(context.getEditLeaveDayDto().getId(), context.getTenantId()));
    }

    @Override
    protected void validate(DataTimeContext<EditLeaveDayDto> context) {
        EditLeaveDayDto editDto = context.getEditLeaveDayDto();
        LeaveDay stored = context.getSingleLeaveDay();

        LocalDate date = resolveRequestedValue(editDto.getDate(), stored.getLeaveDate());
        Double vacation = resolveRequestedValue(editDto.getVacationAmount(), stored.getVacationAmount());
        Double sick = resolveRequestedValue(editDto.getSickAmount(), stored.getSickAmount());

        validateLeaveDayAmounts(date, vacation, sick);

        if (amountOrZero(vacation) + amountOrZero(sick) == 0.0) {
            leaveDayEmpty(String.format(LEAVE_DAY_EMPTY_MSG, date));
        }
        if (isFalse(date.equals(stored.getLeaveDate()))) {
            leaveDaysRepository
                    .findByTenantIdAndIdUserAndLeaveDate(stored.getTenantId(), stored.getIdUser(), date)
                    .ifPresent(clash -> leaveDayDateAlreadyRecorded(
                            String.format(LEAVE_DAY_DATE_ALREADY_RECORDED_MSG, date, stored.getIdUser())));
        }
    }

    @Override
    protected void executeBusiness(DataTimeContext<EditLeaveDayDto> context) {
        EditLeaveDayDto editDto = context.getEditLeaveDayDto();
        LeaveDay leaveDay = context.getSingleLeaveDay();
        ofNullable(editDto.getDate()).ifPresent(leaveDay::setLeaveDate);
        ofNullable(editDto.getVacationAmount())
                .ifPresent(amount -> leaveDay.setVacationAmount(nullIfZero(amount)));
        ofNullable(editDto.getSickAmount())
                .ifPresent(amount -> leaveDay.setSickAmount(nullIfZero(amount)));
    }

    @Override
    protected void mapResult(DataTimeContext<EditLeaveDayDto> context) {
        context.getResponseLeaveDayDto().add(leaveDayMapper.toDto(context.getSingleLeaveDay()));
    }

    private <T> T resolveRequestedValue(T requested, T stored) {
        return requested != null ? requested : stored;
    }
}
