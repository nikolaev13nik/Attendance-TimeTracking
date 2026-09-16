package att.messaging.producer;

import org.springframework.stereotype.Service;

import att.context.AsyncMessageHandler;
import att.context.DataTimeContext;
import att.context.StatisticInfoHolder;
import att.mapper.MonthStatisticMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticAsyncMessageHandler implements AsyncMessageHandler {

    private final StatisticEventProducer statisticEventPublisher;
    private final MonthStatisticMapper monthStatisticMapper;

    @Override
    public void prepareAndSendAsyncStatMsg(DataTimeContext<?> context) {
        StatisticInfoHolder holder = context.getStatisticInfoHolder();
        if (Boolean.TRUE.equals(holder.getReport())) {
            holder.getTargetUserIds().forEach(userId ->
                    statisticEventPublisher.sendMonthStatistic(userId,
                            monthStatisticMapper.toEvent(holder.getMultipleUserData()
                                    .get(userId, holder.getStartOfMonth()))));
        }
    }
}
