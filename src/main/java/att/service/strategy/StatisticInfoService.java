package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.dto.MonthlyUserStatisticInfoDto;
import att.service.base.DataTimeServiceBase;

@Service
public class StatisticInfoService extends DataTimeServiceBase<MonthlyUserStatisticInfoDto> {


    @Override
    protected void fetchAndValidate(DataTimeContext<MonthlyUserStatisticInfoDto> context) {
        super.fetchAndValidate(context);

//        vacationDays	[...] from DB
//        sickDays	[...] from db
    }
}
