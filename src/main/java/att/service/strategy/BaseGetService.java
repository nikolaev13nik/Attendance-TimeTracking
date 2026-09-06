package att.service.strategy;

import java.time.LocalDate;
import java.util.List;

import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

public abstract class BaseGetService<R> extends DataTimeServiceBase<R> {


    List<DataTime> fetchIncompleteSessions(Integer tenantId, Integer idUser, LocalDate startDate, LocalDate endDate) {
        return timeRepository.findIncompleteSessions(tenantId, idUser,
                startDate, endDate);
    }
}
