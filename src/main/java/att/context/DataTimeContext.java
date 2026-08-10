package att.context;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.model.DataTime;
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
public class DataTimeContext<T> {
    // input
    private T task;
    private Integer idUser;
    private Integer recordId;
    private LocalDate workDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private OffsetDateTime openSessionDate;
    private OffsetDateTime closeSessionDate;
    private EditDataTimeUserDto editDto;
    private Integer tenantId;

    @Builder.Default
    private List<DataTime> userWorkSessionList = new ArrayList<>();
    @Builder.Default
    private LocalDate currentLocalDate = LocalDate.now();

    // output
    private Long totalHours;
    private Long totalDays;
    private Long totalOvertimeHours;
    @Builder.Default
    private List<DataTimeDto> responseDataTimeDto = new ArrayList<>();

    public DataTimeDto getSingleResponseDataTimeDto() {
        return this.responseDataTimeDto.get(0);
    }




}