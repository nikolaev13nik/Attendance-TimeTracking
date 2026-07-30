package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

@Service
public class EditRecordService extends DataTimeServiceBase<DataTimeDto> {

    @Override
    protected void fetchAndValidate(DataTimeContext<DataTimeDto> context) {
        context.setDataTime(findRecordOrThrow(context.getEditDto().getId()));
    }

    @Override
    protected void executeBusiness(DataTimeContext<DataTimeDto> context) {
        EditDataTimeUserDto editDto = context.getEditDto();
        DataTime dataTime = context.getDataTime();
        
        if (editDto.getStart() != null) {
            dataTime.setStart(editDto.getStart());
        }
        
        if (editDto.getFinish() != null) {
            dataTime.setFinish(editDto.getFinish());
        }
    }

    @Override
    protected void mapResult(DataTimeContext<DataTimeDto> context) {
        context.setResult(mapper.toDto(context.getDataTime()));
    }
}