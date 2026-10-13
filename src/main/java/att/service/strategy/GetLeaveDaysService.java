package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import att.context.DataTimeContext;
import att.mapper.LeaveDayMapper;
import att.model.LeaveDay;
import att.service.base.DataTimeServiceBase;

@Service
public class GetLeaveDaysService extends DataTimeServiceBase<Void> {

    @Autowired
    private LeaveDayMapper leaveDayMapper;

    @Override
    protected void fetch(DataTimeContext<Void> context) {
        List<LeaveDay> found =
                leaveDaysRepository.findByLeaveDayKeyTenantIdAndLeaveDayKeyIdUserAndLeaveDayKeyLeaveDateBetween(
                        context.getTenantId(), context.getIdUser(), context.getStartDate(), context.getEndDate());
        context.setUserLeaveDaysList(found.stream()
                .sorted(Comparator.comparing((LeaveDay day) -> day.getLeaveDayKey().getLeaveDate()).reversed())
                .toList());
    }

    @Override
    protected void mapResult(DataTimeContext<Void> context) {
        context.setResponseLeaveDayDto(leaveDayMapper.toDtoList(context.getUserLeaveDaysList()));
    }
}
