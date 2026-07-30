package att.service.strategy;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

@Service
public class AddRecordStartService extends DataTimeServiceBase<DataTimeDto> {

    @Override
    protected void executeBusiness(DataTimeContext<DataTimeDto> context) {
        context.setDataTime(new DataTime(context.getUser(), LocalDate.now(), LocalDateTime.now(), null));
    }

    @Override
    protected void persist(DataTimeContext<DataTimeDto> context) {
        context.setDataTime(timeRepository.save(context.getDataTime()));
    }

    @Override
    protected void mapResult(DataTimeContext<DataTimeDto> context) {
        context.setResult(mapper.toDto(context.getDataTime()));
    }
}