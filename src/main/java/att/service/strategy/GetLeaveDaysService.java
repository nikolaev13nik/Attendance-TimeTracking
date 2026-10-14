package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;

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
        leaveDaysRepository.findByTenantIdAndIdUserAndLeaveDateBetween(
                        context.getTenantId(), context.getIdUser(), context.getStartDate(), context.getEndDate())
                .forEach(context::putLeaveDayOnContext);
    }

    @Override
    protected void mapResult(DataTimeContext<Void> context) {
        context.setResponseLeaveDayDto(leaveDayMapper.toDtoList(
                context.getUserLeaveDaysByDate().values().stream()
                        .sorted(Comparator.comparing(LeaveDay::getLeaveDate).reversed())
                        .toList()));
    }
}
