package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.SessionDataDto;
import att.exceptions.ErrorConstants;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.WORK_DATE_MISMATCH_MSG;
import static att.exceptions.ErrorProvider.workDateMismatch;
import static att.service.AttUtility.validateOpenCloseDateInput;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

@Service
public class CloseSessionService extends DataTimeServiceBase<SessionDataDto> {

    @Override
    protected void fetchAndValidate(DataTimeContext<SessionDataDto> context) {
        if (isFalse(context.getWorkDate().equals(context.getCurrentLocalDate()))) {
            workDateMismatch(
                    String.format(WORK_DATE_MISMATCH_MSG, context.getWorkDate(), context.getCurrentLocalDate()));
        }
        validateOpenCloseDateInput(() -> context.getTask().getCloseSessionDate(), context.getWorkDate(),
                String.format(ErrorConstants.OPEN_CLOSE_DATE_MISSING_MSG, context.getTask().getCloseSessionDate()));
        context.setUserWorkSessionList(
                timeRepository.findByTenantIdAndIdUserAndWorkDateOrderByIdDesc(context.getTenantId(),
                        context.getIdUser(), context.getWorkDate()));
    }

    @Override
    protected void executeBusiness(DataTimeContext<SessionDataDto> context) {
        List<DataTime> userWorkSessionList = context.getUserWorkSessionList();
        if (!userWorkSessionList.isEmpty() && userWorkSessionList.get(0).getCloseSessionDate() == null) {
            DataTime dataTime = userWorkSessionList.get(0);
            dataTime.setCloseSessionDate(context.getCloseSessionDate());
//            context.getUserWorkSessionList().add(dataTime);
        } else {
            context.getUserWorkSessionList().clear();
            context.getUserWorkSessionList().add(sessionRecordMapper.mapToDateTime(context));
        }
    }
}