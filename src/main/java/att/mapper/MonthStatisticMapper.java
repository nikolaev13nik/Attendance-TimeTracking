package att.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.YearMonth;

import java.util.List;

import att.client.accounting.dto.UserProfileDto;
import att.context.DataTimeContext;
import att.dto.MonthlyUserStatisticInfoDto;
import att.messaging.dto.MonthStatisticEvent;
import att.messaging.dto.UserStatisticAnalysisRequestEvent;
import att.messaging.dto.UserStatisticHistoryEntryDto;
import att.model.MonthStatistic;

@Mapper(componentModel = "spring")
public interface MonthStatisticMapper {

    @Mapping(target = "tenantId", source = "context.tenantId")
    MonthlyUserStatisticInfoDto toDto(DataTimeContext<?> context, Integer userId, YearMonth yearMonth,
                                      Integer workDays, Double overtimeHours, Double totalWorkHours,
                                      Double vacationDays, Double sickDays);

    @Mapping(target = "monthStatisticKey.tenantId", source = "context.tenantId")
    @Mapping(target = "monthStatisticKey.idUser", source = "userId")
    @Mapping(target = "monthStatisticKey.monthStartDate", source = "monthStart")
    MonthStatistic toEntity(DataTimeContext<?> context, Integer userId, LocalDate monthStart,
                            Integer workDays, Double overtimeHours, Double totalWorkHours,
                            Double vacationDays, Double sickDays);

    @Mapping(target = "yearMonth", expression = "java(YearMonth.from(entity.getMonthStatisticKey().getMonthStartDate()))")
    UserStatisticHistoryEntryDto toHistoryEntry(MonthStatistic entity);

    // every target is mapped explicitly: both sources expose tenantId, which MapStruct would otherwise
    // report as an ambiguous mapping source
    @Mapping(target = "tenantId", source = "dto.tenantId")
    @Mapping(target = "userId", source = "dto.userId")
    @Mapping(target = "yearMonth", source = "dto.yearMonth")
    @Mapping(target = "reportUserName", source = "reportRequester.firstName")
    @Mapping(target = "reportUserLastName", source = "reportRequester.lastName")
    @Mapping(target = "reportEmail", source = "reportRequester.email")
    @Mapping(target = "userName", expression = "java(fullName(targetUser))")
    MonthStatisticEvent toEvent(MonthlyUserStatisticInfoDto dto, UserProfileDto reportRequester,
                                UserProfileDto targetUser);

    default String fullName(UserProfileDto profile) {
        if (profile == null) {
            return null;
        }
        String fullName = "%s %s".formatted(
                profile.getFirstName() == null ? "" : profile.getFirstName(),
                profile.getLastName() == null ? "" : profile.getLastName()).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    /**
     * Forwards the trigger event's identity and report-requester fields onto the analysis request, so the requester
     * resolved once in the statistic pipeline reaches the AI service unchanged.
     */
    @Mapping(target = "entries", source = "entries")
    UserStatisticAnalysisRequestEvent toAnalysisRequest(MonthStatisticEvent source,
                                                        List<UserStatisticHistoryEntryDto> entries);

}
