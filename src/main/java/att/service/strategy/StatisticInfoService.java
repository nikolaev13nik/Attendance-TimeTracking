package att.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import att.client.accounting.api.AccountApi;
import att.client.accounting.dto.UserProfileDto;
import att.context.DataTimeContext;
import att.context.StatisticInfoHolder;
import att.dto.MonthlyUserStatisticInfoDto;
import att.exceptions.BadRequestException;
import att.mapper.MonthStatisticMapper;
import att.model.LeaveDay;
import att.model.MonthStatistic;

import static att.exceptions.ErrorConstants.FULL_LEAVE_DAY_WITH_WORK_MSG;
import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static att.exceptions.ErrorConstants.REPORTER_UNRESOLVED_MSG;
import static att.exceptions.ErrorProvider.fullLeaveDayWithWork;
import static att.service.AttUtility.FULL_DAY;
import static att.service.AttUtility.amountOrZero;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

@Service
public class StatisticInfoService extends BaseGetService<Void> {

    private static final int MINUTES_PER_HOUR = 60;
    private static final Logger logger = LoggerFactory.getLogger(StatisticInfoService.class);

    @Autowired
    private GetWorkedMinutesBetweenService getWorkedMinutesBetweenService;
    @Autowired
    private GetOvertimeMinutesBetweenService getOvertimeMinutesBetweenService;
    @Autowired
    private CountWorkedDaysService countWorkedDaysService;
    @Autowired
    private MonthStatisticMapper monthStatisticMapper;
    @Autowired
    private AccountApi accountApi;

    @Override
    protected void fetch(DataTimeContext<Void> context) {
        YearMonth targetMonth = YearMonth.from(context.getStatisticInfoHolder().getStartOfMonth());
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        context.setStartDate(monthStart);
        context.setEndDate(monthEnd);
        context.getStatisticInfoHolder().setTargetUserIds(resolveTargetUserIds(context));
        fetchFullLeaveDaysWithWork(context);
        resolveTargetUserProfiles(context);
        fetchReporterProfile(context);
    }

    @Override
    protected void validate(DataTimeContext<Void> context) {
        if (isReportRequested(context) && isFalse(hasReporter(context))) {
            throw new BadRequestException(REPORTER_UNRESOLVED_MSG);
        }
        context.getStatisticInfoHolder().getTargetUserIds().forEach(targetUserId -> {
            if (isNotEmpty(fetchIncompleteSessions(context.getTenantId(), targetUserId, context.getStartDate(),
                    context.getEndDate()))) {
                throw new BadRequestException(INCOMPLETE_SESSIONS_MSG);
            }
        });
        List<LeaveDay> fullLeaveDaysWithWork = context.getStatisticInfoHolder().getFullLeaveDaysWithWork();
        if (isNotEmpty(fullLeaveDaysWithWork)) {
            fullLeaveDayWithWork(String.format(FULL_LEAVE_DAY_WITH_WORK_MSG, describeConflicts(fullLeaveDaysWithWork)));
        }
    }

    @Override
    protected void executeBusiness(DataTimeContext<Void> context) {
        YearMonth targetMonth = YearMonth.from(context.getStartDate());
        for (Integer userId : context.getStatisticInfoHolder().getTargetUserIds()) {
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

    private void fetchFullLeaveDaysWithWork(DataTimeContext<Void> context) {
        StatisticInfoHolder holder = context.getStatisticInfoHolder();
        holder.setFullLeaveDaysWithWork(leaveDaysRepository.findFullLeaveDaysWithWorkSessions(context.getTenantId(),
                        context.getStartDate(), context.getEndDate(), FULL_DAY).stream()
                .filter(leaveDay -> holder.getTargetUserIds().contains(leaveDay.getIdUser()))
                .toList());
    }

    private static String describeConflicts(List<LeaveDay> fullLeaveDaysWithWork) {
        return fullLeaveDaysWithWork.stream()
                .collect(groupingBy(LeaveDay::getIdUser, TreeMap::new,
                        mapping(LeaveDay::getLeaveDate, toCollection(TreeSet::new))))
                .entrySet().stream()
                .map(entry -> String.format("user:%s %s", entry.getKey(), entry.getValue()))
                .collect(joining("; "));
    }

    private void resolveTargetUserProfiles(DataTimeContext<Void> context) {
        StatisticInfoHolder holder = context.getStatisticInfoHolder();
        if (Boolean.TRUE.equals(holder.getReport())) {
            try {
                fetchTargetUserProfiles(context).stream()
                        .filter(profile -> holder.getTargetUserIds().contains(profile.getIdUser()))
                        .forEach(profile -> holder.getTargetUserProfiles().put(profile.getIdUser(), profile));
            } catch (RuntimeException e) {
                logger.warn(
                        "Could not resolve names of the reported users of tenant {}, the report will carry ids only",
                        context.getTenantId(), e);
            }
        }
    }

    private List<UserProfileDto> fetchTargetUserProfiles(DataTimeContext<Void> context) {
        if (context.getIdUser() != null) {
            return ofNullable(accountApi.getUser(context.getTenantId(), context.getIdUser()))
                    .map(List::of)
                    .orElseGet(List::of);
        }
        return ofNullable(accountApi.getAllUsers(context.getTenantId())).orElseGet(List::of);
    }

    private void fetchReporterProfile(DataTimeContext<Void> context) {
        if (isReportRequested(context) && hasReporter(context)) {
            StatisticInfoHolder holder = context.getStatisticInfoHolder();
            holder.setReporterProfile(accountApi.getUser(context.getReporterTenantId(), context.getReporterId()));
            logger.info("Reporter {} of tenant {} resolved", context.getReporterId(), context.getReporterTenantId());
        }
    }

    private static boolean isReportRequested(DataTimeContext<Void> context) {
        return Boolean.TRUE.equals(context.getStatisticInfoHolder().getReport());
    }

    private static boolean hasReporter(DataTimeContext<Void> context) {
        return nonNull(context.getReporterId()) && nonNull(context.getReporterTenantId());
    }

    private void populateLeaveDayTotals(DataTimeContext<Void> context, Integer userId) {
        List<LeaveDay> leaveDays = leaveDaysRepository.findByTenantIdAndIdUserAndLeaveDateBetween(
                context.getTenantId(), userId, context.getStartDate(), context.getEndDate());
        context.getVacationPerUser().put(userId, sumLeaveDays(leaveDays, LeaveDay::getVacationAmount));
        context.getSickDayPerUser().put(userId, sumLeaveDays(leaveDays, LeaveDay::getSickAmount));
    }

    private double sumLeaveDays(List<LeaveDay> leaveDays, Function<LeaveDay, Double> portion) {
        return leaveDays.stream()
                .mapToDouble(day -> amountOrZero(portion.apply(day)))
                .sum();
    }
}
