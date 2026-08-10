package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.service.base.DataTimeServiceBase;

@Service
public class GetRecordsByMonthService extends DataTimeServiceBase<Void> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Void> context) {

//        TypedQuery<DataTime> query = em.createQuery(
//            "select h from DataTime h where h.user.idUser=?1 and date BETWEEN :start and :finish",
//            DataTime.class
//        );
//        query.setParameter(1, context.getIdUser());
//        query.setParameter("start", context.getOpenSessionDate());
//        query.setParameter("finish", context.getCloseSessionDate());
//        context.setUserWorkSessionList(query.getResultList());
        context.setUserWorkSessionList(timeRepository.findByUserIdAndWorkDateBetween(context.getTenantId(),
                context.getIdUser(),
                context.getStartDate(), context.getEndDate()));

    }
}