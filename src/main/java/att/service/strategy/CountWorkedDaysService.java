package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.exceptions.BadRequestException;

import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

@Service
public class CountWorkedDaysService extends BaseGetService<Void> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Void> context) {
        if (isNotEmpty(fetchIncompleteSessions(context.getTenantId(), context.getIdUser(),
                context.getStartDate(), context.getEndDate()))) {
            throw new BadRequestException(INCOMPLETE_SESSIONS_MSG);
        }
        context.setTotalDays(
                timeRepository.countByTenantIdAndIdUserAndWorkDateBetween(context.getTenantId(), context.getIdUser(),
                        context.getStartDate(), context.getEndDate()));
    }
}