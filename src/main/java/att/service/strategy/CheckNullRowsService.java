package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.service.base.DataTimeServiceBase;

@Service
public class CheckNullRowsService extends DataTimeServiceBase<List<DataTimeDto>> {

    @Override
    protected void fetchAndValidate(DataTimeContext<List<DataTimeDto>> context) {
//        TypedQuery<DataTime> query = em.createQuery(
//            "select h from DataTime h where h.user.idUser = ?1 and (h.date BETWEEN :start and :finish) and (h.start is null or h.finish is null)",
//            DataTime.class
//        );
//        query.setParameter(1, context.getIdUser());
//        query.setParameter("start", context.getOpenSessionDate());
//        query.setParameter("finish", context.getCloseSessionDate());
//        context.setUserWorkSessionList(query.getResultList());

        context.setUserWorkSessionList(timeRepository.findIncompleteSessions(context.getTenantId(), context.getIdUser(),
                context.getStartDate(), context.getEndDate()));
    }

}