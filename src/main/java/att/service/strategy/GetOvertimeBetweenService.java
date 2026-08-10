package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.service.base.DataTimeServiceBase;

@Service
public class GetOvertimeBetweenService extends DataTimeServiceBase<Long> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Long> context) {

//        Query query = em.createNativeQuery(
//            "select SUM(DATEDIFF(MINUTE, h.start, h.finish) - 480) from hours h where h.id_user=?1 and (h.date BETWEEN ?2 and ?3) and DATEDIFF(MINUTE, h.start, h.finish) > 480"
//        );
//        query.setParameter(1, context.getIdUser());
//        query.setParameter(2, context.getOpenSessionDate());
//        query.setParameter(3, context.getCloseSessionDate());
//
//        Object result = query.getSingleResult();
        Long result = timeRepository.calculateOvertimeMinutes(context.getTenantId(), context.getIdUser(),
                context.getStartDate(),
                context.getEndDate());
        context.setTotalOvertimeHours(result == null ? 0 : result);
    }
}