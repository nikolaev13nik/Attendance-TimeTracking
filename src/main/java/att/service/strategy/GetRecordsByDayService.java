package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;
import jakarta.persistence.TypedQuery;

@Service
public class GetRecordsByDayService extends DataTimeServiceBase<List<DataTimeDto>> {

    @Override
    protected void fetchAndValidate(DataTimeContext<List<DataTimeDto>> context) {
        TypedQuery<DataTime> query = em.createQuery(
            "select h from DataTime h where h.date=?1 ORDER BY id", 
            DataTime.class
        );
        query.setParameter(1, context.getDate());
        context.setDataTimeList(query.getResultList());
    }

    @Override
    protected void mapResult(DataTimeContext<List<DataTimeDto>> context) {
        context.setResult(mapper.toDtoList(context.getDataTimeList()));
    }
}