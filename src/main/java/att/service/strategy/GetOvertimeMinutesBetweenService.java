package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;

@Service
public class GetOvertimeMinutesBetweenService extends BaseGetService<Void> {

    @Override
    protected void fetch(DataTimeContext<Void> context) {
        Long result = timeRepository.calculateOvertimeMinutes(context.getTenantId(), context.getIdUser(),
                context.getStartDate(),
                context.getEndDate());
        context.getTotalOvertimeMinutesPerUser().put(context.getIdUser(), result == null ? 0L : result);
    }
}
