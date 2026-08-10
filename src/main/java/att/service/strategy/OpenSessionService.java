package att.service.strategy;


import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.SessionDataDto;
import att.exceptions.ErrorConstants;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.WORK_DATE_MISMATCH_MSG;
import static att.exceptions.ErrorProvider.workDateMismatch;
import static att.service.AttUtility.validateOpenCloseDateInput;
import static org.apache.commons.lang3.BooleanUtils.isFalse;


@Service
public class OpenSessionService extends DataTimeServiceBase<SessionDataDto> {

    @Override
    protected void executeBusiness(DataTimeContext<SessionDataDto> context) {
        context.getUserWorkSessionList().add(sessionRecordMapper.mapToDateTime(context));
    }

    @Override
    protected void fetchAndValidate(DataTimeContext<SessionDataDto> context) {
        if (isFalse(context.getWorkDate().equals(context.getCurrentLocalDate()))) {
            workDateMismatch(
                    String.format(WORK_DATE_MISMATCH_MSG, context.getWorkDate(), context.getCurrentLocalDate()));
        }
        validateOpenCloseDateInput(() -> context.getTask().getOpenSessionDate(), context.getWorkDate(),
                String.format(ErrorConstants.OPEN_CLOSE_DATE_MISSING_MSG, context.getTask().getOpenSessionDate()));

    }
}