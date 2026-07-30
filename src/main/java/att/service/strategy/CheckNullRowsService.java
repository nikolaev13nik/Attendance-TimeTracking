package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;
import jakarta.persistence.TypedQuery;

@Service
public class CheckNullRowsService extends DataTimeServiceBase<List<DataTimeDto>> {

    @Override
    protected void fetchAndValidate(DataTimeContext<List<DataTimeDto>> context) {
        super.fetchAndValidate(context);
        TypedQuery<DataTime> query = em.createQuery(
            "select h from DataTime h where h.user.idUser = ?1 and (h.date BETWEEN :start and :finish) and (h.start is null or h.finish is null)",
            DataTime.class
        );
        query.setParameter(1, context.getIdUser());
        query.setParameter("start", context.getStartDate());
        query.setParameter("finish", context.getFinishDate());
        context.setDataTimeList(query.getResultList());
    }

    @Override
    protected void mapResult(DataTimeContext<List<DataTimeDto>> context) {
        context.setResult(mapper.toDtoList(context.getDataTimeList()));
    }
}