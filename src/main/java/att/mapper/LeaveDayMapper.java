package att.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.LeaveDayDto;
import att.dto.LeaveDayEntryDto;
import att.model.LeaveDay;

@Mapper(componentModel = "spring")
public interface LeaveDayMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", source = "context.tenantId")
    @Mapping(target = "idUser", source = "context.idUser")
    @Mapping(target = "leaveDate", source = "entry.date")
    @Mapping(target = "vacationAmount", source = "entry.vacationAmount")
    @Mapping(target = "sickAmount", source = "entry.sickAmount")
    LeaveDay toLeaveDay(DataTimeContext<?> context, LeaveDayEntryDto entry);

    @Mapping(target = "userId", source = "idUser")
    @Mapping(target = "date", source = "leaveDate")
    LeaveDayDto toDto(LeaveDay leaveDay);

    List<LeaveDayDto> toDtoList(List<LeaveDay> leaveDays);

}
