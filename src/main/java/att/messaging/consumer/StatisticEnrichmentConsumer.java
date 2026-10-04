package att.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import att.dao.MonthStatisticRepository;
import att.mapper.MonthStatisticMapper;
import att.messaging.dto.MonthStatisticEvent;
import att.messaging.producer.StatisticEventProducer;
import att.model.MonthStatistic;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import static java.util.Optional.ofNullable;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "att.messaging.enrichment.enabled", havingValue = "true", matchIfMissing = true)
public class StatisticEnrichmentConsumer {

    private final MonthStatisticRepository monthStatisticRepository;
    private final MonthStatisticMapper monthStatisticMapper;
    private final StatisticEventProducer statisticEventPublisher;
    private final Validator validator;
    private static final Logger logger = LoggerFactory.getLogger(StatisticEnrichmentConsumer.class);

    @Bean
    public Consumer<Message<MonthStatisticEvent>> monthStatisticEnrichment() {
        return this::monthStatisticEnrichmentHandler;
    }

    private void monthStatisticEnrichmentHandler(Message<MonthStatisticEvent> message) {
        MonthStatisticEvent event = message.getPayload();
        validate(event);
        LocalDate monthStart = event.getYearMonth().atDay(1);
        List<MonthStatistic> lastMonths = monthStatisticRepository
                .findTop7ByMonthStatisticKeyTenantIdAndMonthStatisticKeyIdUserAndMonthStatisticKeyMonthStartDateLessThanEqualOrderByMonthStatisticKeyMonthStartDateDesc(
                        event.getTenantId(), event.getUserId(), monthStart);
        ofNullable(event.getReportEmail()).ifPresentOrElse(reportEmail -> logger.info("Reported Email is found"),
                () -> logger.info("Reported Email is not found"));
        statisticEventPublisher.sendUserStatisticAnalysisRequest(event.getUserId(), monthStatisticMapper
                .toAnalysisRequest(event, lastMonths.stream()
                        .map(monthStatisticMapper::toHistoryEntry)
                        .toList()));
    }


    private void validate(MonthStatisticEvent event) {
        Set<ConstraintViolation<MonthStatisticEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

}
