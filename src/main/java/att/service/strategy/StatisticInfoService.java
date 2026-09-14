package att.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import att.context.DataTimeContext;
import att.dto.MonthlyUserStatisticInfoDto;
import att.exceptions.BadRequestException;
import att.mapper.MonthStatisticMapper;
import att.model.LeaveDay;
import att.model.LeaveType;
import att.model.MonthStatistic;

import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

@Service
public class StatisticInfoService extends BaseGetService<Void> {

    private static final int MINUTES_PER_HOUR = 60;

    @Autowired
    private GetWorkedMinutesBetweenService getWorkedMinutesBetweenService;
    @Autowired
    private GetOvertimeMinutesBetweenService getOvertimeMinutesBetweenService;
    @Autowired
    private CountWorkedDaysService countWorkedDaysService;
    @Autowired
    private MonthStatisticMapper monthStatisticMapper;

    @Override
    protected void fetch(DataTimeContext<Void> context) {
        YearMonth targetMonth = YearMonth.from(context.getStatisticInfoHolder().getStartOfMonth());
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth(); //todo: change to local date in controller ?
        context.setStartDate(monthStart);
        context.setEndDate(monthEnd);
        context.getStatisticInfoHolder().setTargetUserIds(resolveTargetUserIds(context));
    }

    @Override
    protected void validate(DataTimeContext<Void> context) {
        context.getStatisticInfoHolder().getTargetUserIds().forEach(targetUserId -> {
            if (isNotEmpty(fetchIncompleteSessions(context.getTenantId(), targetUserId, context.getStartDate(),
                    context.getEndDate()))) {
                throw new BadRequestException(INCOMPLETE_SESSIONS_MSG);
            }
        });
    }

    @Override
    protected void executeBusiness(DataTimeContext<Void> context) {
        YearMonth targetMonth = YearMonth.from(context.getStartDate());
        for (Integer userId : context.getStatisticInfoHolder().getTargetUserIds()) {
            // reuse the single shared context across every per-user delegate call (instead of one
            // throwaway context per user per metric) - each delegate writes its result keyed by
            // userId into the context's per-user maps, so nothing gets overwritten between iterations
            context.setIdUser(userId);
            countWorkedDaysService.fetch(context);
            getWorkedMinutesBetweenService.fetch(context);
            getOvertimeMinutesBetweenService.fetch(context);
            populateLeaveDayTotals(context, userId);

            composeUserMonthStatistic(context, userId, targetMonth);
        }
    }

    @Override
    protected void mapResult(DataTimeContext<Void> context) {
        // getMonthStatistic reads results directly off context.getStatisticInfoHolder().getMultipleUserData()
    }

    private void composeUserMonthStatistic(DataTimeContext<Void> context, Integer userId, YearMonth targetMonth) {
        Integer workDays = context.getTotalDaysPerUser().get(userId).intValue();
        double totalWorkHours = context.getTotalWorkMinutesPerUser().get(userId) / (double) MINUTES_PER_HOUR;
        double overtimeHours = context.getTotalOvertimeMinutesPerUser().get(userId) / (double) MINUTES_PER_HOUR;
        Double vacationDays = context.getVacationPerUser().get(userId);
        Double sickDays = context.getSickDayPerUser().get(userId);

        MonthlyUserStatisticInfoDto dto = monthStatisticMapper.toDto(context, userId, targetMonth, workDays,
                overtimeHours, totalWorkHours, vacationDays, sickDays);
        context.getStatisticInfoHolder().getMultipleUserData()
                .put(userId, context.getStatisticInfoHolder().getStartOfMonth(), dto);

        MonthStatistic entity = monthStatisticMapper.toEntity(context, userId, context.getStartDate(), workDays,
                overtimeHours, totalWorkHours, vacationDays, sickDays);
        context.getStatisticInfoHolder().getMonthStatisticList().add(entity);
    }

    private Collection<Integer> resolveTargetUserIds(DataTimeContext<Void> context) {
        if (context.getIdUser() != null) {
            return List.of(context.getIdUser());
        }
        Set<Integer> userIds = new TreeSet<>(timeRepository.findDistinctUserIdsByTenantId(context.getTenantId()));
        userIds.addAll(leaveDaysRepository.findDistinctUserIdsByTenantId(context.getTenantId()));
        return userIds;
    }

    private void populateLeaveDayTotals(DataTimeContext<Void> context, Integer userId) {
        List<LeaveDay> leaveDays = leaveDaysRepository
                .findByLeaveDayKeyTenantIdAndLeaveDayKeyIdUserAndLeaveDayKeyLeaveDateBetween(
                        context.getTenantId(), userId, context.getStartDate(), context.getEndDate());
        context.getVacationPerUser().put(userId, sumLeaveDays(leaveDays, LeaveType.VACATION));
        context.getSickDayPerUser().put(userId, sumLeaveDays(leaveDays, LeaveType.SICK));
    }

    private double sumLeaveDays(List<LeaveDay> leaveDays, LeaveType type) {
        return leaveDays.stream()
                .filter(day -> day.getLeaveDayKey().getLeaveType() == type)
                .mapToDouble(LeaveDay::getAmount)
                .sum();
    }
}
