package att.mapper;

import org.mapstruct.Mapper;

import java.util.List;

import att.dto.DataTimeDto;
import att.dto.UserDto;
import att.model.DataTime;
import att.model.User;

@Mapper(componentModel = "spring")
public interface DataTimeMapper {
    DataTimeDto toDto(DataTime dataTime);
    List<DataTimeDto> toDtoList(List<DataTime> list);
    UserDto toUserDto(User user);
}