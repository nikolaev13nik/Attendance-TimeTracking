package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import att.config.WorkTimeConfiguration;
import att.context.DataTimeContext;

@Service
public class GetOvertimeMinutesBetweenService extends BaseGetService<Void> {

    @Autowired
    private WorkTimeConfiguration workTimeConfiguration;

    @Override
    protected void fetch(DataTimeContext<Void> context) {
        Long result = timeRepository.calculateOvertimeMinutes(context.getTenantId(), context.getIdUser(),
                context.getStartDate(),
                context.getEndDate(),
                workTimeConfiguration.getOvertimeDailyThresholdMinutes());
        context.getTotalOvertimeMinutesPerUser().put(context.getIdUser(), result == null ? 0L : result);
    }
}
