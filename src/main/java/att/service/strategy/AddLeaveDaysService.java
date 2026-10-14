package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import att.context.DataTimeContext;
import att.dto.LeaveDayEntryDto;
import att.dto.LeaveReportRequestDto;
import att.mapper.LeaveDayMapper;
import att.model.LeaveDay;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.LEAVE_DAY_DATE_DUPLICATED_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_NOTHING_REPORTED_MSG;
import static att.exceptions.ErrorProvider.leaveDayDateDuplicated;
import static att.exceptions.ErrorProvider.leaveDayNothingReported;
import static att.service.AttUtility.nullIfZero;
import static att.service.AttUtility.validateLeaveDayAmounts;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class AddLeaveDaysService extends DataTimeServiceBase<LeaveReportRequestDto> {

    @Autowired
    private LeaveDayMapper leaveDayMapper;

    @Override
    protected void fetch(DataTimeContext<LeaveReportRequestDto> context) {
        context.setUserLeaveDaysByDate(leaveDaysRepository
                .findByTenantIdAndIdUserAndLeaveDateIn(context.getTenantId(), context.getIdUser(),
                        context.getTask().getDays().stream()
                                .map(LeaveDayEntryDto::getDate)
                                .collect(toSet()))
                .stream()
                .collect(toMap(LeaveDay::getLeaveDate, identity())));
    }

    @Override
    protected void validate(DataTimeContext<LeaveReportRequestDto> context) {
        Set<LocalDate> seen = new HashSet<>();
        context.getTask().getDays().forEach(day -> {
            if (!seen.add(day.getDate())) {
                leaveDayDateDuplicated(String.format(LEAVE_DAY_DATE_DUPLICATED_MSG, day.getDate()));
            }
            if (nullIfZero(day.getVacationAmount()) == null && nullIfZero(day.getSickAmount()) == null) {
                leaveDayNothingReported(String.format(LEAVE_DAY_NOTHING_REPORTED_MSG, day.getDate()));
            }
            LeaveDay storedLeaveDay = context.getUserLeaveDaysByDate().get(day.getDate());
            validateLeaveDayAmounts(day.getDate(),
                    resolveFinalLeaveDayValue(day.getVacationAmount(), storedLeaveDay, LeaveDay::getVacationAmount),
                    resolveFinalLeaveDayValue(day.getSickAmount(), storedLeaveDay, LeaveDay::getSickAmount));
        });
    }

    @Override
    protected void executeBusiness(DataTimeContext<LeaveReportRequestDto> context) {
        Map<LocalDate, LeaveDay> storedByDate = context.getUserLeaveDaysByDate();
        for (LeaveDayEntryDto inputDay : context.getTask().getDays()) {
            LeaveDay leaveDay = storedByDate.get(inputDay.getDate());
            if (leaveDay == null) {
                leaveDay = leaveDayMapper.toLeaveDay(context, inputDay);
            }
            if (inputDay.getVacationAmount() != null) {
                leaveDay.setVacationAmount(nullIfZero(inputDay.getVacationAmount()));
            }
            if (inputDay.getSickAmount() != null) {
                leaveDay.setSickAmount(nullIfZero(inputDay.getSickAmount()));
            }
            context.putLeaveDayOnContext(leaveDay);
        }
    }

    private Double resolveFinalLeaveDayValue(Double requested, LeaveDay storedLeaveDay,
                                             Function<LeaveDay, Double> portion) {
        if (requested != null) {
            return requested;
        }
        return storedLeaveDay == null ? null : portion.apply(storedLeaveDay);
    }
}
