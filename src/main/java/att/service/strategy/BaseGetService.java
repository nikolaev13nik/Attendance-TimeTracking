package att.service.strategy;

import java.time.LocalDate;
import java.util.List;

import att.context.DataTimeContext;
import att.exceptions.BadRequestException;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

public abstract class BaseGetService<R> extends DataTimeServiceBase<R> {


    List<DataTime> fetchIncompleteSessions(Integer tenantId, Integer idUser, LocalDate startDate, LocalDate endDate) {
        return timeRepository.findIncompleteSessions(tenantId, idUser,
                startDate, endDate);
    }

    @Override
    protected void validate(DataTimeContext<R> context) {
        if (isNotEmpty(fetchIncompleteSessions(context.getTenantId(), context.getIdUser(),
                context.getStartDate(), context.getEndDate()))) {
            throw new BadRequestException(INCOMPLETE_SESSIONS_MSG);
        }
    }
}
