package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.service.base.DataTimeServiceBase;

@Service
public class CheckNullRowsService extends DataTimeServiceBase<List<DataTimeDto>> {

    @Override
    protected void fetchAndValidate(DataTimeContext<List<DataTimeDto>> context) {
        context.setUserWorkSessionList(timeRepository.findIncompleteSessions(context.getTenantId(), context.getIdUser(),
                context.getStartDate(), context.getEndDate()));
    }

}