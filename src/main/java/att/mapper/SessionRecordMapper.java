package att.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import att.context.DataTimeContext;
import att.dto.DataTimeDto;

import att.model.DataTime;


@Mapper(componentModel = "spring")
public interface SessionRecordMapper {


    @Mapping(target = "userId", source = "idUser")
    DataTimeDto toDto(DataTime dataTime);

    List<DataTimeDto> toDtoList(List<DataTime> list);

    DataTime mapToDateTime(DataTimeContext<?> context);


}