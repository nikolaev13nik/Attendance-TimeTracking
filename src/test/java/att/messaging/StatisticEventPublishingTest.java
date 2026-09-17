package att.messaging;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import att.controller.BaseApiControllerTest;
import att.messaging.dto.MonthStatisticEvent;

import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("Statistic API with report=true publishes the expected month-statistic event")
class StatisticEventPublishingTest extends BaseApiControllerTest {

    private void closeBlockingSession() {
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());
    }

    @Test
    @FlywayTest
    @DisplayName("report=true publishes one trigger event carrying the computed statistic of the reported user")
    void reportTrue_singleUser_publishesExpectedEventTest() {
        closeBlockingSession();
        // leave days make vacationDays/sickDays non-zero, so the payload assertions below cover
        // every field instead of passing trivially on defaults
        ResponseEntity<String> leaveResponse = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-25"), 0.5, 0.5),
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-26"), null, 0.5),
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-28"), null, 0.5),
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-29"), 0.5, null))));
        assertEquals(HttpStatus.OK, leaveResponse.getStatusCode());

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                withReport(statistic(STATISTIC_URL, "2024-01", USER_ID), true), null, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<MonthStatisticEvent> captor = ArgumentCaptor.forClass(MonthStatisticEvent.class);
        verify(statisticEventProducer).sendMonthStatistic(eq(USER_ID), captor.capture());
        MonthStatisticEvent event = captor.getValue();
        assertEquals(2, event.getTenantId());
        assertEquals(USER_ID, event.getUserId());
        assertEquals(YearMonth.parse("2024-01"), event.getYearMonth());
        assertEquals(3, event.getWorkDays());
        assertEquals(25.0, event.getTotalWorkHours());
        assertEquals(1.0, event.getOvertimeHours());
        assertEquals(1.0, event.getVacationDays());
        assertEquals(1.5, event.getSickDays());

        verify(statisticEventProducer, never()).sendUserStatisticAnalysisRequest(any(), any());
    }

    @Test
    @FlywayTest
    @DisplayName("report=true with idUser omitted publishes one trigger event per tenant user")
    void reportTrue_allUsers_publishesOnePerUserTest() {
        closeBlockingSession();
        // OTHER_USER_ID has no seeded work sessions under tenant 2, so it only shows up in
        // resolveTargetUserIds() once it has a leave day - mirrors getMonthStatisticAllUsersTest
        ResponseEntity<String> leaveUser3 = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, OTHER_USER_ID, 2,
                generateLeaveReportRequestDto(List.of(
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-10"), 1.0, null))));
        assertEquals(HttpStatus.OK, leaveUser3.getStatusCode());

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                withReport(statistic(STATISTIC_URL, "2024-01"), true), null, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<MonthStatisticEvent> captor = ArgumentCaptor.forClass(MonthStatisticEvent.class);
        verify(statisticEventProducer, times(2)).sendMonthStatistic(anyInt(), captor.capture());

        MonthStatisticEvent user2Event = captor.getAllValues().stream()
                .filter(e -> USER_ID.equals(e.getUserId())).findFirst().orElseThrow();
        assertEquals(2, user2Event.getTenantId());
        assertEquals(YearMonth.parse("2024-01"), user2Event.getYearMonth());
        assertEquals(3, user2Event.getWorkDays());
        assertEquals(25.0, user2Event.getTotalWorkHours());
        assertEquals(1.0, user2Event.getOvertimeHours());
        assertEquals(0.0, user2Event.getVacationDays());
        assertEquals(0.0, user2Event.getSickDays());

        MonthStatisticEvent user3Event = captor.getAllValues().stream()
                .filter(e -> OTHER_USER_ID.equals(e.getUserId())).findFirst().orElseThrow();
        assertEquals(2, user3Event.getTenantId());
        assertEquals(YearMonth.parse("2024-01"), user3Event.getYearMonth());
        assertEquals(0, user3Event.getWorkDays());
        assertEquals(0.0, user3Event.getTotalWorkHours());
        assertEquals(0.0, user3Event.getOvertimeHours());
        assertEquals(1.0, user3Event.getVacationDays());
        assertEquals(0.0, user3Event.getSickDays());

        verify(statisticEventProducer, never()).sendUserStatisticAnalysisRequest(any(), any());
    }

    @Test
    @FlywayTest
    @DisplayName("report=true but the statistic flow is rejected - nothing is published")
    void reportTrue_rejectedRequest_publishesNothingTest() {
        long countBefore = monthStatisticRepository.count();

        // the blocking session is deliberately left open, so the pipeline fails before the
        // post-service action that would publish
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                withReport(statistic(STATISTIC_URL, "2024-01", USER_ID), true), null, 2, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(INCOMPLETE_SESSIONS_MSG, errorMessage(response));

        assertEquals(countBefore, monthStatisticRepository.count(),
                "Reason: no statistic row should be persisted for a rejected request");
        verifyNoInteractions(statisticEventProducer);
    }
}
