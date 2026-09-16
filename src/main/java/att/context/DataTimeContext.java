package att.context;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.model.DataTime;
import att.model.LeaveDay;
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
public class DataTimeContext<T> implements BusinessStrategyContext {

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
    private List<LeaveDay> userLeaveDaysList = new ArrayList<>();
    @Builder.Default
    private LocalDate currentLocalDate = LocalDate.now();
    // output - per user, keyed by idUser, so one context can be reused across a multi-user pipeline
    // (e.g. StatisticInfoService) without one user's result overwriting another's
    @Builder.Default
    Map<Integer, Long> totalDaysPerUser = new HashMap<>();
    @Builder.Default
    Map<Integer, Long> totalWorkMinutesPerUser = new HashMap<>();
    @Builder.Default
    Map<Integer, Long> totalOvertimeMinutesPerUser = new HashMap<>();
    @Builder.Default
    Map<Integer, Double> vacationPerUser = new HashMap<>();
    @Builder.Default
    Map<Integer, Double> sickDayPerUser = new HashMap<>();

    @Builder.Default
    private List<DataTimeDto> responseDataTimeDto = new ArrayList<>();
    @Builder.Default
    private StatisticInfoHolder statisticInfoHolder = new StatisticInfoHolder();

    private AsyncMessageHandler asyncMessageHandler;

    public DataTimeDto getSingleResponseDataTimeDto() {
        return this.responseDataTimeDto.get(0);
    }

    @Override
    public <S extends BusinessStrategyContext> Consumer<S> getPostServiceAction() {
        return ctx -> doPostServiceAction((DataTimeContext<?>) ctx);
    }

    private void doPostServiceAction(DataTimeContext<?> context) {
        context.asyncMessageHandler.prepareAndSendAsyncStatMsg(context);
    }
}