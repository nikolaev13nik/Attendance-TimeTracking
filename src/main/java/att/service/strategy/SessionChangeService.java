package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.EditDataTimeUserDto;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

import static java.util.Optional.ofNullable;

@Service
public class SessionChangeService extends DataTimeServiceBase<EditDataTimeUserDto> {

    @Override
    protected void fetchAndValidate(DataTimeContext<EditDataTimeUserDto> context) {
        context.getUserWorkSessionList().add(findRecordOrThrow(context.getEditDto().getId()));
    }

    @Override
    protected void executeBusiness(DataTimeContext<EditDataTimeUserDto> context) {
        EditDataTimeUserDto editDto = context.getEditDto();
        DataTime dataTime = context.getUserWorkSessionList().get(0);
        ofNullable(editDto.getOpenSessionDate()).ifPresent(dataTime::setOpenSessionDate);
        ofNullable(editDto.getCloseSessionDate()).ifPresent(dataTime::setCloseSessionDate);
    }

    @Override
    protected void mapResult(DataTimeContext<EditDataTimeUserDto> context) {
        context.getResponseDataTimeDto().add(sessionRecordMapper.toDto(context.getUserWorkSessionList().get(0)));
    }
}