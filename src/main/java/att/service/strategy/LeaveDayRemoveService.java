package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.LeaveDayDto;
import att.mapper.LeaveDayMapper;
import att.service.base.DataTimeServiceBase;

@Service
public class LeaveDayRemoveService extends DataTimeServiceBase<LeaveDayDto> {

    @Autowired
    private LeaveDayMapper leaveDayMapper;

    @Override
    protected void fetch(DataTimeContext<LeaveDayDto> context) {
        context.putLeaveDayOnContext(findLeaveDayOrThrow(context.getLeaveDayId(), context.getTenantId()));
    }

    @Override
    protected void persist(DataTimeContext<LeaveDayDto> context) {
        leaveDaysRepository.deleteAll(context.getUserLeaveDaysByDate().values());
    }

    @Override
    protected void mapResult(DataTimeContext<LeaveDayDto> context) {
        context.getResponseLeaveDayDto().add(leaveDayMapper.toDto(context.getSingleLeaveDay()));
    }
}
