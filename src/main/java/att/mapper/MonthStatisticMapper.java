package att.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.YearMonth;

import java.util.List;

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

    MonthStatisticEvent toEvent(MonthlyUserStatisticInfoDto dto);

    UserStatisticAnalysisRequestEvent toAnalysisRequest(Integer tenantId, Integer userId,
                                                        List<UserStatisticHistoryEntryDto> entries);

}
