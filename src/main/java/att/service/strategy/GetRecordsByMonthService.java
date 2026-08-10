package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.service.base.DataTimeServiceBase;

@Service
public class GetRecordsByMonthService extends DataTimeServiceBase<Void> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Void> context) {
        context.setUserWorkSessionList(timeRepository.findByUserIdAndWorkDateBetween(context.getTenantId(),
                context.getIdUser(),
                context.getStartDate(), context.getEndDate()));

    }
}