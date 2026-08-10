package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.EditDataTimeUserDto;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

@Service
public class EditRecordService extends DataTimeServiceBase<EditDataTimeUserDto> {

    @Override
    protected void fetchAndValidate(DataTimeContext<EditDataTimeUserDto> context) {
        context.getUserWorkSessionList().add(findRecordOrThrow(context.getEditDto().getId()));
    }

    @Override
    protected void executeBusiness(DataTimeContext<EditDataTimeUserDto> context) {
        EditDataTimeUserDto editDto = context.getEditDto();
        DataTime dataTime = context.getUserWorkSessionList().get(0);

        if (editDto.getOpenSessionDate() != null) {
            dataTime.setOpenSessionDate(editDto.getOpenSessionDate());
        }

        if (editDto.getCloseSessionDate() != null) {
            dataTime.setCloseSessionDate(editDto.getCloseSessionDate());
        }
    }

    @Override
    protected void mapResult(DataTimeContext<EditDataTimeUserDto> context) {
        context.getResponseDataTimeDto().add(sessionRecordMapper.toDto(context.getUserWorkSessionList().get(0)));
    }
}