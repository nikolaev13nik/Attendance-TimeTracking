package att.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;

import att.context.DataTimeContext;
import att.dto.LeaveDayDto;
import att.model.LeaveDay;
import att.model.LeaveType;

@Mapper(componentModel = "spring")
public interface LeaveDayMapper {

    @Mapping(target = "leaveDayKey.tenantId", source = "context.tenantId")
    @Mapping(target = "leaveDayKey.idUser", source = "context.idUser")
    @Mapping(target = "leaveDayKey.leaveDate", source = "date")
    @Mapping(target = "leaveDayKey.leaveType", source = "type")
    LeaveDay toLeaveDay(DataTimeContext<?> context, LocalDate date, LeaveType type, double amount);

    @Mapping(target = "tenantId", source = "leaveDayKey.tenantId")
    @Mapping(target = "userId", source = "leaveDayKey.idUser")
    @Mapping(target = "date", source = "leaveDayKey.leaveDate")
    @Mapping(target = "leaveType", source = "leaveDayKey.leaveType")
    LeaveDayDto toDto(LeaveDay leaveDay);

    List<LeaveDayDto> toDtoList(List<LeaveDay> leaveDays);

}
