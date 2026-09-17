package att.messaging;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import att.controller.BaseApiControllerTest;
import att.mapper.MonthStatisticMapper;
import att.messaging.consumer.StatisticEnrichmentConsumer;
import att.messaging.dto.MonthStatisticEvent;
import att.messaging.dto.UserStatisticAnalysisRequestEvent;
import att.messaging.dto.UserStatisticHistoryEntryDto;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("monthStatisticEnrichment consumer: enriches a MonthStatisticEvent and forwards it to the AI service")
class StatisticEnrichmentConsumerTest extends BaseApiControllerTest {

    @Autowired
    private MonthStatisticMapper monthStatisticMapper;

    @Autowired
    private Validator validator;

    private Consumer<Message<MonthStatisticEvent>> enrichmentConsumer() {
        return new StatisticEnrichmentConsumer(monthStatisticRepository, monthStatisticMapper,
                statisticEventProducer, validator).monthStatisticEnrichment();
    }

    /**
     * The history under test comes from V3__att_month_statistic_setup.sql (tenant 2 / SEEDED_STAT_USER_ID).
     */
    private UserStatisticAnalysisRequestEvent sendMsToEnrichConsumer() {
        MonthStatisticEvent event = MonthStatisticEvent.builder()
                .tenantId(2)
                .userId(SEEDED_STAT_USER_ID)
                .yearMonth(YearMonth.parse(SEEDED_STAT_TARGET_MONTH))
                .build();

        enrichmentConsumer().accept(MessageBuilder.withPayload(event).build());

        ArgumentCaptor<UserStatisticAnalysisRequestEvent> captor =
                ArgumentCaptor.forClass(UserStatisticAnalysisRequestEvent.class);
        verify(statisticEventProducer).sendUserStatisticAnalysisRequest(eq(SEEDED_STAT_USER_ID), captor.capture());
        return captor.getValue();
    }

    @Test
    @FlywayTest
    @DisplayName("enrichment history is anchored on the event's yearMonth, not just the newest rows in the table")
    void enrichment_anchorsHistoryOnTargetYearMonthTest() {
        UserStatisticAnalysisRequestEvent analysis = sendMsToEnrichConsumer();
        assertEquals(2, analysis.getTenantId());
        assertEquals(SEEDED_STAT_USER_ID, analysis.getUserId());

        List<YearMonth> entryMonths = analysis.getEntries().stream()
                .map(entry -> entry.getYearMonth()).toList();
        assertEquals(7, entryMonths.size(),
                "Reason: current month + the 6 most recent months before it, anchored on the target yearMonth");
        assertEquals(YearMonth.parse(SEEDED_STAT_TARGET_MONTH), entryMonths.get(0),
                "Reason: entries are ordered most-recent-first, starting with the current month");
        assertTrue(entryMonths.containsAll(List.of(YearMonth.parse("2023-07"), YearMonth.parse("2023-08"),
                        YearMonth.parse("2023-09"), YearMonth.parse("2023-10"), YearMonth.parse("2023-11"),
                        YearMonth.parse(SEEDED_STAT_PREVIOUS_MONTH))),
                "Reason: the 6 months immediately before the target must all be present");
        assertFalse(entryMonths.contains(YearMonth.parse(SEEDED_STAT_OLDEST_EXCLUDED_MONTH)),
                "Reason: 2023-06 is the 7th-oldest month and must be excluded once there are more than 6 prior months");
        assertFalse(entryMonths.contains(YearMonth.parse(SEEDED_STAT_FUTURE_MONTH)),
                "Reason: a month after the target yearMonth must never appear in the history");
        // the seed carries the same months under tenant 3 / user 8, so any break in the tenant+user scoping
        // of the consumer query shows up here as a duplicated month
        assertEquals(entryMonths.size(), Set.copyOf(entryMonths).size(),
                String.format("Reason: rows of tenant %s or user %s must never leak into the history, %s",
                        SEEDED_STAT_OTHER_TENANT_ID, SEEDED_STAT_OTHER_USER_ID, entryMonths));
    }

    @Test
    @FlywayTest
    @DisplayName("enrichment carries the persisted statistic values of every history entry")
    void enrichment_entriesCarryPersistedStatisticValuesTest() {
        List<UserStatisticHistoryEntryDto> entries = sendMsToEnrichConsumer().getEntries();

        UserStatisticHistoryEntryDto current = entries.get(0);
        assertEquals(YearMonth.parse(SEEDED_STAT_TARGET_MONTH), current.getYearMonth());
        assertEquals(22, current.getWorkDays());
        assertEquals(4.0, current.getOvertimeHours());
        assertEquals(175.0, current.getTotalWorkHours());
        assertEquals(1.0, current.getVacationDays());
        assertEquals(0.5, current.getSickDays());

        UserStatisticHistoryEntryDto previous = entries.get(1);
        assertEquals(YearMonth.parse(SEEDED_STAT_PREVIOUS_MONTH), previous.getYearMonth());
        assertEquals(21, previous.getWorkDays());
        assertEquals(3.0, previous.getOvertimeHours());
        assertEquals(168.0, previous.getTotalWorkHours());
        assertEquals(2.0, previous.getVacationDays());
        assertEquals(1.5, previous.getSickDays());
    }

    @Test
    @DisplayName("a malformed trigger event (missing userId) is rejected and never forwarded to the AI service")
    void enrichment_invalidEvent_isRejectedAndNotForwardedTest() {
        MonthStatisticEvent malformed = MonthStatisticEvent.builder()
                .tenantId(999)
                .yearMonth(YearMonth.parse("2024-01"))
                .build();

        // the binder turns this into a DLQ routing (async-messages.yml marks ConstraintViolationException
        // non-retryable for monthStatisticEnrichment-in-0)
        assertThrows(ConstraintViolationException.class,
                () -> enrichmentConsumer().accept(MessageBuilder.withPayload(malformed).build()),
                "Reason: a message missing the mandatory userId must be rejected, not silently dropped");

        verifyNoInteractions(statisticEventProducer);
    }
}
