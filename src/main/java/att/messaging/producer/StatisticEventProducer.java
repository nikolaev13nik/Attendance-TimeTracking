package att.messaging.producer;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

import att.messaging.MessagingConstants;
import att.messaging.dto.MonthStatisticEvent;
import att.messaging.dto.UserStatisticAnalysisRequestEvent;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticEventProducer {

    private final StreamBridge streamBridge;

    public void sendMonthStatistic(Integer userId, MonthStatisticEvent event) {
        streamBridge.send(MessagingConstants.MONTH_STATISTIC_PRODUCER_BINDING, keyedMessage(userId, event));
    }

    public void sendUserStatisticAnalysisRequest(Integer userId, UserStatisticAnalysisRequestEvent event) {
        streamBridge.send(MessagingConstants.STATISTIC_AI_ANALYSIS_PRODUCER_BINDING, keyedMessage(userId, event));
    }

    private <T> Message<T> keyedMessage(Integer userId, T payload) {
        return MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.KEY, String.valueOf(userId).getBytes(StandardCharsets.UTF_8))
                .build();
    }
}
