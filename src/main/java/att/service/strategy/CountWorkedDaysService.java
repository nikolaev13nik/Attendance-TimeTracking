package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;

@Service
public class CountWorkedDaysService extends BaseGetService<Void> {

    @Override
    protected void fetch(DataTimeContext<Void> context) {
        Long days = timeRepository.countWorkedDaysBetween(context.getTenantId(),
                context.getIdUser(), context.getStartDate(), context.getEndDate());
        context.getTotalDaysPerUser().put(context.getIdUser(), days);
    }
}