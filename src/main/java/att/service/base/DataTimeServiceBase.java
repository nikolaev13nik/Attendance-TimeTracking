package att.service.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

import att.context.DataTimeContext;
import att.dao.LeaveDaysRepository;
import att.dao.MonthStatisticRepository;
import att.dao.SessionAttendanceTimeRepository;
import att.exceptions.ErrorConstants;
import att.exceptions.NotFoundException;
import att.mapper.SessionRecordMapper;
import att.model.DataTime;
import att.model.LeaveDay;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import co.il.attendance.logging.LogContextScope;
import jakarta.transaction.Transactional;

import static co.il.attendance.logging.LogContext.TARGET_USER_ID;

public abstract class DataTimeServiceBase<R> implements BaseService<R> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected SessionAttendanceTimeRepository timeRepository;
    @Autowired
    protected LeaveDaysRepository leaveDaysRepository;
    @Autowired
    protected SessionRecordMapper sessionRecordMapper;
    @Autowired
    private MonthStatisticRepository monthStatisticRepository;

    @PersistenceContext
    protected EntityManager em;

    @Transactional
    @Override
    public void execute(DataTimeContext<R> context) {
        executeWithoutTransactional(context);
    }

    @Override
    public void executeWithoutTransactional(DataTimeContext<R> context) {
        try (LogContextScope ignored = LogContextScope.open().with(TARGET_USER_ID, context.getIdUser())) {
            try {
                fetch(context);
                validate(context);
                executeBusiness(context);
                persist(context);
                mapResult(context);
                log.info("Api completed");
            } catch (RuntimeException e) {
                logFailure(e);
                throw e;
            }
        }
    }

    protected void fetch(DataTimeContext<R> context) {
    }

    protected void validate(DataTimeContext<R> context) {
    }

    protected void executeBusiness(DataTimeContext<R> context) {
    }

    protected void persist(DataTimeContext<R> context) {
        timeRepository.saveAll(context.getUserWorkSessionList());
        leaveDaysRepository.saveAll(context.getUserLeaveDaysByDate().values());
        monthStatisticRepository.saveAll(context.getStatisticInfoHolder().getMonthStatisticList());
    }

    protected void mapResult(DataTimeContext<R> context) {
        context.setResponseDataTimeDto(sessionRecordMapper.toDtoList(context.getUserWorkSessionList()));
    }

    protected DataTime findRecordOrThrow(Integer id, Integer tenantId) {
        return timeRepository.findByIdAndTenantId(id, tenantId).orElseThrow(
                () -> new NotFoundException(String.format(ErrorConstants.ATTENDANCE_NOT_FOUND_MSG, id)));
    }

    protected LeaveDay findLeaveDayOrThrow(Integer id, Integer tenantId) {
        return leaveDaysRepository.findByIdAndTenantId(id, tenantId).orElseThrow(
                () -> new NotFoundException(String.format(ErrorConstants.LEAVE_DAY_NOT_FOUND_MSG, id)));
    }

    private void logFailure(RuntimeException e) {
        ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class);
        if (status != null && status.code().is4xxClientError()) {
            log.warn("rejected: {}", status.reason().isEmpty() ? e.getMessage() : status.reason());
            return;
        }
        log.error("failed", e);
    }
}
