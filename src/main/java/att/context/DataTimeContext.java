package att.context;

import java.time.LocalDate;
import java.util.List;

import att.dto.EditDataTimeUserDto;
import att.model.DataTime;
import att.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataTimeContext<R> {
    // input
    private Integer idUser;
    private Integer recordId;
    private LocalDate date;
    private LocalDate startDate;
    private LocalDate finishDate;
    private EditDataTimeUserDto editDto;

    // working state
    private User user;
    private DataTime dataTime;
    private List<DataTime> dataTimeList;

    // output
    private R result;
}