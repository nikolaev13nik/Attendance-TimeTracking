package att.service.strategy;


import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.SessionDataDto;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.WORK_DATE_MISMATCH_MSG;
import static att.exceptions.ErrorProvider.workDateMismatch;
import static org.apache.commons.lang3.BooleanUtils.isFalse;


@Service
public class OpenSessionService extends DataTimeServiceBase<SessionDataDto> {

    @Override
    protected void executeBusiness(DataTimeContext<SessionDataDto> context) {
        context.getUserWorkSessionList().add(sessionRecordMapper.mapToDateTime(context));
    }

    @Override
    protected void fetchAndValidate(DataTimeContext<SessionDataDto> context) {
        if (isFalse(context.getWorkDate().equals(context.getNow()))) {
            workDateMismatch(String.format(WORK_DATE_MISMATCH_MSG, context.getWorkDate(), context.getNow()));
        }
        // to add validation for user id , required accounting service
    }
}