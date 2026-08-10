package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.service.base.DataTimeServiceBase;

@Service
public class RemoveRecordService extends DataTimeServiceBase<DataTimeDto> {

    @Override
    protected void fetchAndValidate(DataTimeContext<DataTimeDto> context) {
        context.getUserWorkSessionList().add(findRecordOrThrow(context.getRecordId()));
    }

    @Override
    protected void persist(DataTimeContext<DataTimeDto> context) {
        timeRepository.deleteAll(context.getUserWorkSessionList());
    }

}