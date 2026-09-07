package att.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

import att.context.DataTimeContext;
import att.model.LeaveDay;
import att.model.LeaveType;

@Mapper(componentModel = "spring")
public interface LeaveDayMapper {

    @Mapping(target = "leaveDayKey.tenantId", source = "context.tenantId")
    @Mapping(target = "leaveDayKey.idUser", source = "context.idUser")
    @Mapping(target = "leaveDayKey.leaveDate", source = "date")
    @Mapping(target = "leaveDayKey.leaveType", source = "type")
    LeaveDay toLeaveDay(DataTimeContext<?> context, LocalDate date, LeaveType type, double amount);

}
