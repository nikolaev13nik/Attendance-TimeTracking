package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.service.base.DataTimeServiceBase;

@Service
public class GetOvertimeBetweenService extends DataTimeServiceBase<Long> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Long> context) {
        Long result = timeRepository.calculateOvertimeMinutes(context.getTenantId(), context.getIdUser(),
                context.getStartDate(),
                context.getEndDate());
        context.setTotalOvertimeHours(result == null ? 0 : result);
    }
}