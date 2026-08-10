package att.service.base;

import org.springframework.beans.factory.annotation.Autowired;

import att.context.DataTimeContext;
import att.dao.SessionAttendanceTimeRepository;
import att.exceptions.ErrorConstants;
import att.exceptions.NotFoundException;
import att.mapper.SessionRecordMapper;
import att.model.DataTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

public abstract class DataTimeServiceBase<R> implements BaseService<R> {

    @Autowired
    protected SessionAttendanceTimeRepository timeRepository;

    @Autowired
    protected SessionRecordMapper sessionRecordMapper;

    @PersistenceContext
    protected EntityManager em;

    @Transactional
    @Override
    public void execute(DataTimeContext<R> context) {
        executeWithoutTransactional(context);
    }

    @Override
    public void executeWithoutTransactional(DataTimeContext<R> context) {
        fetchAndValidate(context);
        executeBusiness(context);
        persist(context);
        mapResult(context);
    }

    protected void fetchAndValidate(DataTimeContext<R> context) {
    }

    protected void executeBusiness(DataTimeContext<R> context) {
    }

    protected void persist(DataTimeContext<R> context) {
        timeRepository.saveAll(context.getUserWorkSessionList());
    }

    protected void mapResult(DataTimeContext<R> context) {
        context.setResponseDataTimeDto(sessionRecordMapper.toDtoList(context.getUserWorkSessionList()));
    }

    protected DataTime findRecordOrThrow(Integer id) {
        return timeRepository.findById(id).orElseThrow(
                () -> new NotFoundException(String.format(ErrorConstants.ATTENDANCE_NOT_FOUND_MSG, id)));
    }
}