package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;

@Service
public class CheckNullRowsService extends BaseGetService<List<DataTimeDto>> {

    @Override
    protected void fetchAndValidate(DataTimeContext<List<DataTimeDto>> context) {
        context.setUserWorkSessionList(fetchIncompleteSessions(context.getTenantId(), context.getIdUser(),
                context.getStartDate(), context.getEndDate()));
    }


//    List<DataTime> fetchIncompleteSessions(Integer tenantId, Integer idUser, LocalDate startDate, LocalDate endDate){
//        return  timeRepository.findIncompleteSessions(tenantId, idUser,
//                startDate,endDate);
//    }

}