package att.service.strategy;

import org.springframework.stereotype.Service;

import att.context.DataTimeContext;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;
import jakarta.persistence.TypedQuery;

@Service
public class GetRecordsByDayService extends DataTimeServiceBase<Void> {

    @Override
    protected void fetchAndValidate(DataTimeContext<Void> context) {
        TypedQuery<DataTime> query = em.createQuery(
            "select h from DataTime h where h.date=?1 ORDER BY id", 
            DataTime.class
        );
        query.setParameter(1, context.getWorkDate());
        context.setUserWorkSessionList(query.getResultList());
    }

}