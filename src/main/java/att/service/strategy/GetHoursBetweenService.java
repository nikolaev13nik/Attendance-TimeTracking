package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.service.base.DataTimeServiceBase;

@Service
public class GetHoursBetweenService extends DataTimeServiceBase<Long> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Long> context) {
        Long countMinutes = timeRepository.calculateWorkedMinutes(context.getTenantId(), context.getIdUser(),
                context.getStartDate(),
                context.getEndDate());
        context.setTotalHours(countMinutes == null ? 0 : countMinutes);
    }
}