package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import att.context.DataTimeContext;
import att.dto.LeaveReportRequestDto;
import att.mapper.LeaveDayMapper;
import att.model.LeaveDay;
import att.model.LeaveType;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG;
import static att.exceptions.ErrorProvider.leaveDayAmountExceedsFullDay;

@Service
public class AddLeaveDaysService extends DataTimeServiceBase<LeaveReportRequestDto> {

    @Autowired
    private LeaveDayMapper leaveDayMapper;

    @Override
    protected void fetchAndValidate(DataTimeContext<LeaveReportRequestDto> context) {
        context.getTask().getDays().forEach(day -> {
            double vacation = day.getVacationAmount() != null ? day.getVacationAmount() : 0.0;
            double sick = day.getSickAmount() != null ? day.getSickAmount() : 0.0;
            if (vacation + sick > 1.0) {
                leaveDayAmountExceedsFullDay(String.format(LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG,
                        day.getDate(), day.getVacationAmount(), day.getSickAmount()));
            }
        });
    }

    @Override
    protected void executeBusiness(DataTimeContext<LeaveReportRequestDto> context) {
        List<LeaveDay> leaveDays = new ArrayList<>();
        context.getTask().getDays().forEach(day -> {
            if (day.getVacationAmount() != null) {
                leaveDays.add(
                        leaveDayMapper.toLeaveDay(context, day.getDate(), LeaveType.VACATION, day.getVacationAmount()));
            }
            if (day.getSickAmount() != null) {
                leaveDays.add(leaveDayMapper.toLeaveDay(context, day.getDate(), LeaveType.SICK, day.getSickAmount()));
            }
        });
        context.setUserLeaveDaysList(leaveDays);
    }

    @Override
    protected void mapResult(DataTimeContext<LeaveReportRequestDto> context) {
        // addLeaveDays returns ResponseEntity<Void> - nothing to map back
    }
}
