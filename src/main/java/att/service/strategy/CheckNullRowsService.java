package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;

@Service
public class CheckNullRowsService extends BaseGetService<List<DataTimeDto>> {

    @Override
    protected void fetch(DataTimeContext<List<DataTimeDto>> context) {
        context.setUserWorkSessionList(fetchIncompleteSessions(context.getTenantId(), context.getIdUser(),
                context.getStartDate(), context.getEndDate()));
    }

    @Override
    protected void validate(DataTimeContext<List<DataTimeDto>> context) {
        // no need validation
    }
}