package att.service.strategy;


import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.SessionDataDto;
import att.exceptions.ErrorConstants;
import att.service.base.DataTimeServiceBase;

import static att.service.AttUtility.validateOpenCloseDateInput;


@Service
public class OpenSessionService extends DataTimeServiceBase<SessionDataDto> {

    @Override
    protected void executeBusiness(DataTimeContext<SessionDataDto> context) {
        context.getUserWorkSessionList().add(sessionRecordMapper.mapToDateTime(context));
    }

    @Override
    protected void fetch(DataTimeContext<SessionDataDto> context) {
        validateOpenCloseDateInput(() -> context.getTask().getOpenSessionDate(), context.getWorkDate(),
                String.format(ErrorConstants.OPEN_CLOSE_DATE_MISSING_MSG, context.getTask().getOpenSessionDate()));

    }
}