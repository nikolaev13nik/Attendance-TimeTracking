package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.service.base.DataTimeServiceBase;

@Service
public class CountWorkedDaysService extends DataTimeServiceBase<Void> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Void> context) {
        context.setTotalDays(
                timeRepository.countByTenantIdAndIdUserAndWorkDateBetween(context.getTenantId(), context.getIdUser(),
                        context.getStartDate(), context.getEndDate()));
    }
}