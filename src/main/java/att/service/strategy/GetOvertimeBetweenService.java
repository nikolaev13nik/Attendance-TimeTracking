package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.exceptions.BadRequestException;

import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

@Service
public class GetOvertimeBetweenService extends BaseGetService<Long> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Long> context) {
        if (isNotEmpty(fetchIncompleteSessions(context.getTenantId(), context.getIdUser(),
                context.getStartDate(), context.getEndDate()))) {
            throw new BadRequestException(INCOMPLETE_SESSIONS_MSG);
        }
        Long result = timeRepository.calculateOvertimeMinutes(context.getTenantId(), context.getIdUser(),
                context.getStartDate(),
                context.getEndDate());
        context.setTotalOvertimeHours(result == null ? 0 : result);
    }
}