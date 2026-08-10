package att.service.strategy;

import org.springframework.stereotype.Service;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.SessionDataDto;
import att.model.DataTime;
import att.service.base.DataTimeServiceBase;

import static att.exceptions.ErrorConstants.WORK_DATE_MISMATCH_MSG;
import static att.exceptions.ErrorProvider.workDateMismatch;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

@Service
public class CloseSessionService extends DataTimeServiceBase<SessionDataDto> {

    @Override
    protected void fetchAndValidate(DataTimeContext<SessionDataDto> context) {
        if (isFalse(context.getWorkDate().equals(context.getNow()))) {
            workDateMismatch(String.format(WORK_DATE_MISMATCH_MSG, context.getWorkDate(), context.getNow()));
        }
        context.setUserWorkSessionList(
                timeRepository.findByTenantIdAndIdUserAndWorkDateOrderByIdDesc(context.getTenantId(),
                        context.getIdUser(), context.getWorkDate()));
//        TypedQuery<DataTime> query = em.createQuery(
//                "select h from DataTime h where h.user.idUser = ?1 and h.date = ?2 ORDER BY id DESC",
//                DataTime.class);
//        query.setParameter(1, context.getIdUser());
//        query.setParameter(2, LocalDate.now());
//        context.setDataTimeList(query.getResultList());
    }

    @Override
    protected void executeBusiness(DataTimeContext<SessionDataDto> context) {
        List<DataTime> userWorkSessionList = context.getUserWorkSessionList();
        if (!userWorkSessionList.isEmpty() && userWorkSessionList.get(0).getCloseSessionDate() == null) {
            DataTime dataTime = userWorkSessionList.get(0);
            dataTime.setCloseSessionDate(context.getCloseSessionDate());
//            context.getUserWorkSessionList().add(dataTime);
        } else {
            context.getUserWorkSessionList().clear();
            context.getUserWorkSessionList().add(sessionRecordMapper.mapToDateTime(context));
        }
    }
}