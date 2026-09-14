package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;

@Service
public class GetWorkedMinutesBetweenService extends BaseGetService<Void> {

    @Override
    protected void fetch(DataTimeContext<Void> context) {
        Long countMinutes = timeRepository.calculateWorkedMinutes(context.getTenantId(), context.getIdUser(),
                context.getStartDate(),
                context.getEndDate());
        context.getTotalWorkMinutesPerUser().put(context.getIdUser(), countMinutes == null ? 0L : countMinutes);
    }
}
