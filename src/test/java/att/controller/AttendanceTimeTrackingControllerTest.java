package att.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import att.dto.DataTimeDto;
import att.dto.LeaveDayDto;
import att.dto.LeaveDayEntryDto;
import att.dto.LeaveReportRequestDto;
import att.dto.MonthlyUserStatisticInfoDto;
import att.dto.SessionDataDto;
import att.model.DataTime;
import att.model.LeaveDay;

import static att.exceptions.ErrorConstants.ATTENDANCE_NOT_FOUND_MSG;
import static att.exceptions.ErrorConstants.FUTURE_WORK_DATE_MSG;
import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_DATE_ALREADY_RECORDED_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_EMPTY_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_NOTHING_REPORTED_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_NOT_FOUND_MSG;
import static att.exceptions.ErrorConstants.OPEN_CLOSE_DATE_MISSING_MSG;
import static att.exceptions.ErrorConstants.PREVIOUS_DAY_REQUIRES_ADMIN_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

class AttendanceTimeTrackingControllerTest extends BaseApiControllerTest {

    @Test
    @FlywayTest
    @DisplayName("start record ,the same user opens a record- missing JWT's tenantId  - negative")
    void openNewAttendanceSessionTest_missingTenantId_negative() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime openDate = OffsetDateTime.now();
        SessionDataDto task = generateSessionDataDto(LocalDate.now(), openDate, null);
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, OPEN_URL, 123, tenant,
                task);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(errorMessage(response).contains("Request was rejected: lack of tenant ID in security context"));
        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("start record ,the same user opens a record- path tenant Id dont match JWT's tenantId  - negative")
    void openNewAttendanceSessionTest_JwtTenantDontMatchPath_negative() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime openDate = OffsetDateTime.now();
        SessionDataDto task = generateSessionDataDto(LocalDate.now(), openDate, null);
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, OPEN_URL, 2, tenant,
                task, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(errorMessage(response).contains("Request was rejected: JWT's tenant does not match to path"));
        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("start record ,the same user opens a record - positive")
    void openNewAttendanceSessionTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime openDate = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        SessionDataDto task = generateSessionDataDto(LocalDate.now(), openDate, null);
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, OPEN_URL, USER_ID, tenant,
                task);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // response validation
        DataTimeDto body = readObject(response, DataTimeDto.class);
        verifyRespondedBodyOpenCloseSessionApi(body, USER_ID, tenant, openDate, null, null);
        // db state validations
        verifyRecordStartEndApi(body.getId(), USER_ID, body.getOpenSessionDate(), null);
        assertEquals(recordCountBefore + 1, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record is to be increased abd expect to be as:%s", recordCountBefore + 1));
    }

    @Test
    @FlywayTest
    @DisplayName("start record ,the same user opens a record - positive")
    void openNewAttendanceSession_missingOpenDateTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        SessionDataDto task = generateSessionDataDto(LocalDate.now(), null, null);
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, OPEN_URL, USER_ID, tenant,
                task);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(String.format(OPEN_CLOSE_DATE_MISSING_MSG, null, null), errorMessage(response));
        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("user opens a record for today in their own offset, whatever the server zone - positive")
    void openWorkSession_userTodayInOwnOffset_positive() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 123;
        for (ZoneOffset offset : List.of(ZoneOffset.ofHours(14), ZoneOffset.ofHours(-12))) {
            OffsetDateTime openDate = OffsetDateTime.now(offset).truncatedTo(ChronoUnit.MILLIS);
            SessionDataDto task = generateSessionDataDto(openDate.toLocalDate(), openDate, null);
            ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, OPEN_URL, 123, tenant,
                    task, jwtTokenUserTenant_123);
            assertEquals(HttpStatus.OK, response.getStatusCode(),
                    String.format("Reason: work date:%s is today in the caller's offset:%s", openDate.toLocalDate(),
                            offset));
            DataTimeDto body = readObject(response, DataTimeDto.class);
            assertEquals(openDate.toLocalDate(), body.getWorkDate(),
                    "Reason: the record must keep the caller's calendar day");
        }
        assertEquals(recordCountBefore + 2, sessionAttendanceTimeRepository.count(),
                "Reason: one record per accepted request is expected");
    }

    @Test
    @FlywayTest
    @DisplayName("user opens a record for a previous day returns 400 - negative")
    void openWorkSession_userPreviousDay_negative() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 123;
        OffsetDateTime openDate = OffsetDateTime.now().minusDays(1);
        LocalDate previousDay = openDate.toLocalDate();
        SessionDataDto task = generateSessionDataDto(previousDay, openDate, null);
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, OPEN_URL, 123, tenant,
                task, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Reason: only an administrator may record a previous day");
        assertEquals(String.format(PREVIOUS_DAY_REQUIRES_ADMIN_MSG, previousDay, previousDay.plusDays(1)),
                errorMessage(response), "Reason: the rejection must name the previous-day rule");
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                "Reason: a rejected request must not persist a record");
    }

    @Test
    @FlywayTest
    @DisplayName("user closes a record for a previous day returns 400 - negative")
    void closeWorkSession_userPreviousDay_negative() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 123;
        OffsetDateTime closeDate = OffsetDateTime.now().minusDays(1);
        LocalDate previousDay = closeDate.toLocalDate();
        SessionDataDto task = generateSessionDataDto(previousDay, null, closeDate);
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, CLOSE_URL, 123, tenant,
                task, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Reason: only an administrator may record a previous day");
        assertEquals(String.format(PREVIOUS_DAY_REQUIRES_ADMIN_MSG, previousDay, previousDay.plusDays(1)),
                errorMessage(response), "Reason: the rejection must name the previous-day rule");
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                "Reason: a rejected request must not persist a record");
    }

    @Test
    @FlywayTest
    @DisplayName("administrator opens and closes a record for a previous day - positive")
    void openCloseWorkSession_adminPreviousDay_positive() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime openDate = OffsetDateTime.now().minusDays(3).truncatedTo(ChronoUnit.MILLIS);
        OffsetDateTime closeDate = openDate.plusMinutes(1);
        LocalDate previousDay = openDate.toLocalDate();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, OPEN_URL, OTHER_USER_ID, tenant,
                generateSessionDataDto(previousDay, openDate, null));
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Reason: an administrator may open a record for a previous day");
        DataTimeDto opened = readObject(response, DataTimeDto.class);

        response = sendRequestWithAdmin(HttpMethod.PUT, CLOSE_URL, OTHER_USER_ID, tenant,
                generateSessionDataDto(previousDay, null, closeDate));
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Reason: an administrator may close a record for a previous day");
        DataTimeDto closed = readObject(response, DataTimeDto.class);
        verifyRespondedBodyOpenCloseSessionApi(closed, OTHER_USER_ID, tenant, openDate, closeDate, opened.getId());
        verifyRecordStartEndApi(closed.getId(), OTHER_USER_ID, openDate, closeDate);
        assertEquals(previousDay, closed.getWorkDate(), "Reason: the record must keep the requested previous day");
        assertEquals(recordCountBefore + 1, sessionAttendanceTimeRepository.count(),
                "Reason: closing must reuse the open record of that day instead of inserting");
    }

    @Test
    @FlywayTest
    @DisplayName("administrator opens a record for a future day returns 400 - negative")
    void openWorkSession_adminFutureDay_negative() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime openDate = OffsetDateTime.now().plusDays(1);
        LocalDate futureDay = openDate.toLocalDate();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, OPEN_URL, OTHER_USER_ID, tenant,
                generateSessionDataDto(futureDay, openDate, null));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Reason: nobody may record a future day");
        assertEquals(String.format(FUTURE_WORK_DATE_MSG, futureDay, futureDay.minusDays(1)), errorMessage(response),
                "Reason: the rejection must name the future-day rule");
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                "Reason: a rejected request must not persist a record");
    }

    @Test
    @FlywayTest
    @DisplayName("close work session, closes the open record for today (reused, not inserted), then insert - positive")
    void closeWorkSessionTest_positive() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime closeSessionDate = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDate inputWorkDate = closeSessionDate.toLocalDate();
        SessionDataDto task = generateSessionDataDto(inputWorkDate, null, closeSessionDate);
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, CLOSE_URL,
                USER_ID, tenant, task);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // response validation
        DataTimeDto body = readObject(response, DataTimeDto.class);
        verifyRespondedBodyOpenCloseSessionApi(body, USER_ID, tenant, null, closeSessionDate, SEEDED_TODAY_OPEN_ID);
        // db state validations
        verifyRecordStartEndApi(body.getId(), USER_ID, body.getOpenSessionDate(), body.getCloseSessionDate());
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed,and expect to be as:%s", recordCountBefore));

        // when user sends by mistake new close attendance
        // when last one was already closed
        // we expect to insert new record
        OffsetDateTime secondCloseSessionDate = closeSessionDate.plusHours(1);
        task = generateSessionDataDto(inputWorkDate, null, secondCloseSessionDate);
        response = sendRequestWithAdmin(HttpMethod.PUT, CLOSE_URL, USER_ID, tenant, task);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // response validation
        body = readObject(response, DataTimeDto.class);
        verifyRespondedBodyOpenCloseSessionApi(body, USER_ID, tenant, null,
                secondCloseSessionDate, null);
        // db state validations
        verifyRecordStartEndApi(body.getId(), USER_ID, body.getOpenSessionDate(), body.getCloseSessionDate());
        assertEquals(recordCountBefore + 1, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed,and" +
                        " expect to be as:%s", recordCountBefore + 1));

    }

    @Test
    @FlywayTest
    @DisplayName("edit record , admin edits an existing record - positive")
    void editRecordAsAdminTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        OffsetDateTime newFinishTime = OffsetDateTime.parse("2026-01-03T18:00:00Z");
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_FINISHED_ID,
                null, newFinishTime));
        assertEquals (HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject (response, DataTimeDto.class);
        assertEquals(newFinishTime, body.getCloseSessionDate());

        // db state validations
        OffsetDateTime closeSessionDateDB = sessionAttendanceTimeRepository.findById(SEEDED_FINISHED_ID).orElseThrow()
                .getCloseSessionDate();

        assertEquals(18,
                closeSessionDateDB.getHour(), String.format("Reason: expect to see close date as:%s  but not as:%s",
                        newFinishTime, closeSessionDateDB));
        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("edit record for a missing record id returns 400")
    void editRecordMissingTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        OffsetDateTime newFinishTime = OffsetDateTime.parse("2024-01-03T18:00:00Z");
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 999,
                createEditDataTimeUserDto(999999, null, newFinishTime));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(String.format(ATTENDANCE_NOT_FOUND_MSG, 999999), errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

//    @Test
//    @FlywayTest
//    @DisplayName("GET /record/records?localDate=... as admin returns that day's records")
//    void recordsByDayAsAdminTest() {
//        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, RECORDS_URL + "?localDate=" + SEEDED_DAY,
//                null,
//                2, null);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId).toList();
//        assertTrue(ids.contains(1001));
//    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records as admin returns the user's records in range")
    void rangeRecordsAsAdminTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(RANGE_RECORDS_URL, USER_ID), null,
                2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId ).toList();
        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of (1001, 1002, 1003)));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records for a wromg user id")
    void rangeRecordsMissingUserTest() {
        int fakeUserId = 987654;
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET,
                range(RANGE_RECORDS_URL, fakeUserId), null,
                2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId).toList();
        assertEquals(0, ids.size());

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/workdays as admin counts distinct worked days")
    void workdaysAsAdminTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();

        // negative: SEEDED_OPEN_ID (1003) is unclosed and falls inside the queried range
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(WORKDAYS_URL, USER_ID), null,
                2, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(INCOMPLETE_SESSIONS_MSG, errorMessage(response));

        // close the blocking session via the edit API, then the identical request now succeeds
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());

        response = sendRequestWithAdmin(HttpMethod.GET, range(WORKDAYS_URL, USER_ID), null,
                2, null);
        assertEquals (HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, readObject(response, Long.class).longValue());

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET get total minutes as admin sums worked minutes")
    void getTotalMinutesAsAdminTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();

        // negative: SEEDED_OPEN_ID (1003) is unclosed and falls inside the queried range
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(MINUTES_URL, USER_ID), null,
                2, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(INCOMPLETE_SESSIONS_MSG, errorMessage(response));

        // close the blocking session via the edit API, then the identical request now succeeds
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());

        response = sendRequestWithAdmin(HttpMethod.GET, range(MINUTES_URL, USER_ID), null,
                2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1500L, readObject(response, Long.class).longValue());

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtimeMinutes as admin sums minutes above 480/day")
    void overtimeMinutesAsAdminTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();

        // negative: SEEDED_OPEN_ID (1003) is unclosed and falls inside the queried range
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_MINUTES_URL, USER_ID),
                null, 2, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(INCOMPLETE_SESSIONS_MSG, errorMessage(response));

        // close the blocking session via the edit API, then the identical request now succeeds
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());

        response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_MINUTES_URL, USER_ID), null,
                2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(60L, readObject(response, Long.class).longValue());

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET check incomplete sessions as admin returns rows with an open start/finish")
    void checkIncompleteSessions_AsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(CHECK_URL), USER_ID, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId).toList();
        assertEquals(1, ids.size());
        assertTrue(ids.contains(SEEDED_OPEN_ID));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE session as admin removes the record")
    void removeSessionAsAdminTest() {
        ResponseEntity<String> response = sendDeleteRequestWithAdmin(HttpMethod.DELETE, REMOVE_URL,
                SEEDED_FINISHED_ID, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject(response, DataTimeDto.class);
        assertEquals(SEEDED_FINISHED_ID, body.getId().intValue());
        assertFalse(sessionAttendanceTimeRepository.existsById(SEEDED_FINISHED_ID));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE  session for a missing record returns 400")
    void removeSessionMissingTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendDeleteRequestWithAdmin(HttpMethod.DELETE, REMOVE_URL,
                999999, 2, null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(String.format(ATTENDANCE_NOT_FOUND_MSG, 999999), errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("add leave days as admin - positive")
    void addLeaveDaysTest() {
        List<LeaveDayEntryDto> days = List.of(
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-15"), 1.0, null),
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-16"), 0.5, 0.5),
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-20"), null, 1.0));
        LeaveReportRequestDto task = generateLeaveReportRequestDto(days);

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2, task);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<LeaveDay> saved = leaveDaysRepository.findByTenantIdAndIdUser(2, USER_ID).stream()
                .filter(d -> d.getLeaveDate().getMonthValue() == 1)
                .toList();
        assertEquals(3, saved.size(), "Reason: one row per reported date");
        assertTrue(saved.stream().anyMatch(d -> d.getLeaveDate().equals(LocalDate.parse("2024-01-15"))
                && d.getVacationAmount() == 1.0 && d.getSickAmount() == null));
        assertTrue(saved.stream().anyMatch(d -> d.getLeaveDate().equals(LocalDate.parse("2024-01-16"))
                        && d.getVacationAmount() == 0.5 && d.getSickAmount() == 0.5),
                "Reason: half vacation plus half sick share a single row for the date");
        assertTrue(saved.stream().anyMatch(d -> d.getLeaveDate().equals(LocalDate.parse("2024-01-20"))
                && d.getVacationAmount() == null && d.getSickAmount() == 1.0));

        // negative case
        LocalDate reportDate = LocalDate.parse(
                "2024-01-20");
        LeaveReportRequestDto task2 = generateLeaveReportRequestDto(List.of(generateLeaveDayEntryDto(reportDate,
                1.0, 0.5)));

        ResponseEntity<String> response2 = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2, task2);
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        assertEquals(String.format(LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG, reportDate, 1.0, 0.5),
                errorMessage(response2));
    }

    @Test
    @FlywayTest
    @DisplayName("POST month statistic as admin - single user")
    void getMonthStatisticSingleUser_withVacationAndSickDaysTest() {
        // preparation for test
        // close the blocking session so the month has no incomplete sessions
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());

        LeaveReportRequestDto leaveReport = generateLeaveReportRequestDto(List.of(
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-25"), 0.5, 0.5),
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-26"), null, 0.5),
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-28"), null, 0.5),
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-29"), 0.5, null)));
        ResponseEntity<String> leaveResponse =
                sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2, leaveReport);
        assertEquals(HttpStatus.OK, leaveResponse.getStatusCode());

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                statistic(STATISTIC_URL, "2024-01", USER_ID), null, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<MonthlyUserStatisticInfoDto> body = readList(response, MonthlyUserStatisticInfoDto.class);
        assertEquals(1, body.size());
        MonthlyUserStatisticInfoDto stat = body.get(0);
        assertEquals(2, stat.getTenantId());
        assertEquals(USER_ID, stat.getUserId());
        assertEquals(YearMonth.parse("2024-01"), stat.getYearMonth());
        assertEquals(3, stat.getWorkDays());
        assertEquals(25.0, stat.getTotalWorkHours());
        assertEquals(1.0, stat.getOvertimeHours());
        assertEquals(1.0, stat.getVacationDays());
        assertEquals(1.5, stat.getSickDays());

        // db state validation
        verifyMonthStatisticDbState(stat, 2);
    }

    @Test
    @FlywayTest
    @DisplayName("POST month statistic as admin - report=false computes the statistic without publishing anything")
    void getMonthStatisticReportFalse_publishesNothingTest() {
        // close the blocking session so the month has no incomplete sessions
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                withReport(statistic(STATISTIC_URL, "2024-01", USER_ID), false), null, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        MonthlyUserStatisticInfoDto stat = readList(response, MonthlyUserStatisticInfoDto.class).get(0);
        assertEquals(USER_ID, stat.getUserId());
        assertEquals(3, stat.getWorkDays());
        assertEquals(25.0, stat.getTotalWorkHours());
        verifyMonthStatisticDbState(stat, 2);

        verifyNoInteractions(statisticEventProducer);
        // the report flag gates the Accounting lookup too, not only the publishing
        verifyNoInteractions(accountApi);
    }

    @Test
    @FlywayTest
    @DisplayName("POST month statistic as admin - incomplete session rejects the request")
    void getMonthStatisticIncompleteSessionTest() {
        long countBefore = monthStatisticRepository.count();

        // SEEDED_OPEN_ID (1003) is left unclosed and falls inside the queried month
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                statistic(STATISTIC_URL, "2024-01", USER_ID), null, 2, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(INCOMPLETE_SESSIONS_MSG, errorMessage(response));

        assertEquals(countBefore, monthStatisticRepository.count(),
                "Reason: no statistic row should be persisted for a rejected request");
    }

    @Test
    @FlywayTest
    @DisplayName("POST month statistic as admin - idUser omitted covers every user seeded under the tenant")
    void getMonthStatisticAllUsersTest() {
        long countBefore = monthStatisticRepository.count();

        // close the blocking session so user 2's month has no incomplete sessions
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());

        // user 2: worked sessions + leave days
        ResponseEntity<String> leaveUser2 = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-25"), 1.0, null),
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-26"), null, 1.0))));
        assertEquals(HttpStatus.OK, leaveUser2.getStatusCode());

        // user 3 (OTHER_USER_ID): no worked sessions under this tenant, only leave days
        ResponseEntity<String> leaveUser3 = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, OTHER_USER_ID, 2,
                generateLeaveReportRequestDto(List.of(
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-10"), 1.0, null),
                        generateLeaveDayEntryDto(LocalDate.parse("2024-01-11"), null, 1.0))));
        assertEquals(HttpStatus.OK, leaveUser3.getStatusCode());

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                statistic(STATISTIC_URL, "2024-01"), null, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<MonthlyUserStatisticInfoDto> body = readList(response, MonthlyUserStatisticInfoDto.class);
        assertEquals(2, body.size());

        MonthlyUserStatisticInfoDto user2Stat =
                body.stream().filter(s -> USER_ID.equals(s.getUserId())).findFirst().orElseThrow();
        assertEquals(3, user2Stat.getWorkDays());
        assertEquals(25.0, user2Stat.getTotalWorkHours());
        assertEquals(1.0, user2Stat.getOvertimeHours());
        assertEquals(1.0, user2Stat.getVacationDays());
        assertEquals(1.0, user2Stat.getSickDays());

        MonthlyUserStatisticInfoDto user3Stat =
                body.stream().filter(s -> OTHER_USER_ID.equals(s.getUserId())).findFirst().orElseThrow();
        assertEquals(0, user3Stat.getWorkDays());
        assertEquals(0.0, user3Stat.getTotalWorkHours());
        assertEquals(0.0, user3Stat.getOvertimeHours());
        assertEquals(1.0, user3Stat.getVacationDays());
        assertEquals(1.0, user3Stat.getSickDays());

        // db state validations
        verifyMonthStatisticDbState(user2Stat, 2);
        verifyMonthStatisticDbState(user3Stat, 2);
        assertEquals(countBefore + 2, monthStatisticRepository.count(),
                "Reason: exactly one row per covered user should be added by this flow");
        verifyNoInteractions(statisticEventProducer);
    }

    @Test
    @FlywayTest
    @DisplayName("POST month statistic as admin - idUser omitted, one incomplete user aborts the whole batch")
    void getMonthStatisticAllUsersIncompleteAbortsTest() {
        long countBefore = monthStatisticRepository.count();

        // SEEDED_OPEN_ID (1003) is left unclosed for user 2 under tenant 2
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST,
                statistic(STATISTIC_URL, "2024-01"), null, 2, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(INCOMPLETE_SESSIONS_MSG, errorMessage(response));

        assertEquals(countBefore, monthStatisticRepository.count(),
                "Reason: no statistic row should be persisted when the batch is rejected");
    }

    @Test
    @FlywayTest
    @DisplayName("POST month statistic as admin - recomputing after a new session updates the persisted row without a duplicate-key failure")
    void getMonthStatisticRecomputeAfterNewSessionTest() {
        long countBefore = monthStatisticRepository.count();

        // close the blocking session so the month has no incomplete sessions
        ResponseEntity<String> editResponse = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL,
                null, 2, createEditDataTimeUserDto(SEEDED_OPEN_ID, null,
                        OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());

        // first call: computes and persists the initial MonthStatistic row
        ResponseEntity<String> firstResponse = sendRequestWithAdmin(HttpMethod.POST,
                statistic(STATISTIC_URL, "2024-01", USER_ID), null, 2, null);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        MonthlyUserStatisticInfoDto firstStat = readList(firstResponse, MonthlyUserStatisticInfoDto.class).get(0);
        assertEquals(3, firstStat.getWorkDays());
        assertEquals(25.0, firstStat.getTotalWorkHours());
        verifyMonthStatisticDbState(firstStat, 2);
        assertEquals(countBefore + 1, monthStatisticRepository.count(),
                "Reason: the first call should add exactly one row");

        // add a brand-new attendance record for the same tenant/user/month directly via the repository:
        // openSession/closeSession only allow workDate == "today" (see OpenSessionService/CloseSessionService
        // workDateMismatch check), so a historical-month record can't be added through those endpoints in a test
        DataTime newSession = new DataTime();
        newSession.setTenantId(2);
        newSession.setIdUser(USER_ID);
        newSession.setWorkDate(LocalDate.parse("2024-01-15"));
        newSession.setOpenSessionDate(OffsetDateTime.parse("2024-01-15T09:00:00Z"));
        newSession.setCloseSessionDate(OffsetDateTime.parse("2024-01-15T17:00:00Z"));
        sessionAttendanceTimeRepository.save(newSession);

        // second call: same tenant/user/month - must upsert the existing PK row, not insert a duplicate
        ResponseEntity<String> secondResponse = sendRequestWithAdmin(HttpMethod.POST,
                statistic(STATISTIC_URL, "2024-01", USER_ID), null, 2, null);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode(),
                "Reason: recomputing an already-persisted month/tenant/user must not fail on the att_month_statistic primary key");
        MonthlyUserStatisticInfoDto secondStat = readList(secondResponse, MonthlyUserStatisticInfoDto.class).get(0);

        // values must reflect the newly added session
        assertEquals(4, secondStat.getWorkDays());
        assertEquals(33.0, secondStat.getTotalWorkHours());
        assertNotEquals(firstStat.getWorkDays(), secondStat.getWorkDays());
        assertNotEquals(firstStat.getTotalWorkHours(), secondStat.getTotalWorkHours());

        // still exactly one row for this key - upsert, not a duplicate insert
        verifyMonthStatisticDbState(secondStat, 2);
        assertEquals(countBefore + 1, monthStatisticRepository.count(),
                "Reason: recomputation must update the existing row in place, not add a second one");
    }

    @Test
    @FlywayTest
    @DisplayName("GET /attendance/workdays counts a date carrying two sessions as one worked day")
    void workdaysCountsDistinctDatesTest() {
        ResponseEntity<String> closeBlocking = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL, null, 2,
                createEditDataTimeUserDto(SEEDED_OPEN_ID, null, OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, closeBlocking.getStatusCode());

        ResponseEntity<String> before = sendRequestWithAdmin(HttpMethod.GET, range(WORKDAYS_URL, USER_ID),
                null, 2, null);
        assertEquals(HttpStatus.OK, before.getStatusCode());
        assertEquals(3L, readObject(before, Long.class).longValue(),
                "Reason: the seeded user has sessions on three distinct dates inside the range");

        DataTime secondSessionSameDay = new DataTime();
        secondSessionSameDay.setTenantId(2);
        secondSessionSameDay.setIdUser(USER_ID);
        secondSessionSameDay.setWorkDate(LocalDate.parse(SEEDED_DAY));
        secondSessionSameDay.setOpenSessionDate(OffsetDateTime.parse("2024-01-02T18:30:00Z"));
        secondSessionSameDay.setCloseSessionDate(OffsetDateTime.parse("2024-01-02T20:00:00Z"));
        sessionAttendanceTimeRepository.save(secondSessionSameDay);

        ResponseEntity<String> after = sendRequestWithAdmin(HttpMethod.GET, range(WORKDAYS_URL, USER_ID),
                null, 2, null);
        assertEquals(HttpStatus.OK, after.getStatusCode());
        assertEquals(3L, readObject(after, Long.class).longValue(),
                "Reason: a second session on " + SEEDED_DAY + " is a break within one worked day, not a fourth day");
    }

    @Test
    @FlywayTest
    @DisplayName("edit record for a session owned by another tenant returns 404 and leaves it unchanged")
    void editRecordOtherTenantTest() {
        OffsetDateTime closeBefore = sessionAttendanceTimeRepository.findById(SEEDED_FINISHED_ID)
                .orElseThrow().getCloseSessionDate();

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL, null, 1,
                createEditDataTimeUserDto(SEEDED_FINISHED_ID, null,
                        OffsetDateTime.parse("2024-01-03T23:00:00Z")));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Reason: session " + SEEDED_FINISHED_ID + " belongs to tenant 2 and must be invisible under tenant 1");
        assertEquals(String.format(ATTENDANCE_NOT_FOUND_MSG, SEEDED_FINISHED_ID), errorMessage(response));

        OffsetDateTime closeAfter = sessionAttendanceTimeRepository.findById(SEEDED_FINISHED_ID)
                .orElseThrow().getCloseSessionDate();
        assertEquals(closeBefore.toInstant(), closeAfter.toInstant(),
                "Reason: a rejected cross-tenant edit must not write to the other tenant's session");
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE session owned by another tenant returns 404 and keeps the session")
    void removeSessionOtherTenantTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();

        ResponseEntity<String> response = sendDeleteRequestWithAdmin(HttpMethod.DELETE, REMOVE_URL,
                SEEDED_FINISHED_ID, 1, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Reason: session " + SEEDED_FINISHED_ID + " belongs to tenant 2 and must be invisible under tenant 1");
        assertEquals(String.format(ATTENDANCE_NOT_FOUND_MSG, SEEDED_FINISHED_ID), errorMessage(response));
        assertTrue(sessionAttendanceTimeRepository.existsById(SEEDED_FINISHED_ID),
                "Reason: a rejected cross-tenant delete must leave the other tenant's session in place");
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                "Reason: no row may be removed by a cross-tenant delete");
    }

    @Test
    @FlywayTest
    @DisplayName("an employee may read their own sessions without being an administrator")
    void sessionsForSelfAsUserTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(RECORDS_URL, USER_ID),
                null, 2, null, jwtTokenSeededUser);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Reason: people must be able to see the hours recorded against their own name");
        assertFalse(readList(response, DataTimeDto.class).isEmpty(),
                "Reason: the seeded employee has sessions inside the queried range");
    }

    @Test
    @FlywayTest
    @DisplayName("an employee may not read another employee's sessions")
    void sessionsForOtherUserAsUserTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(RECORDS_URL, OTHER_USER_ID),
                null, 2, null, jwtTokenSeededUser);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "Reason: self-service must not become a way to read a colleague's attendance");
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("an employee may check their own incomplete sessions")
    void checkForSelfAsUserTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET,
                String.format(CHECK_URL, 2, USER_ID) + "?startDate=" + RANGE_START + "&endDate=" + RANGE_END,
                null, null, null, jwtTokenSeededUser);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Reason: the session screen shows which of your own rows still need fixing");
    }

    @Test
    @FlywayTest
    @DisplayName("an employee may record and then read back their own leave days, most recent first")
    void leaveDaysForSelfAsUserTest() {
        List<LeaveDayEntryDto> days = List.of(
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-08"), 1.0, null),
                generateLeaveDayEntryDto(LocalDate.parse("2024-01-22"), null, 0.5));
        ResponseEntity<String> added = sendRequestWithUserRole(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(days), jwtTokenSeededUser);
        assertEquals(HttpStatus.OK, added.getStatusCode(),
                "Reason: people record their own holiday and sick leave, not only administrators");

        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET,
                String.format(LEAVE_DAYS_URL, 2, USER_ID) + "?startDate=" + RANGE_START + "&endDate=" + RANGE_END,
                null, null, null, jwtTokenSeededUser);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<LeaveDayDto> body = readList(response, LeaveDayDto.class);
        assertEquals(2, body.size(), "Reason: both recorded days fall inside the queried range");
        assertEquals(LocalDate.parse("2024-01-22"), body.get(0).getDate(),
                "Reason: the most recent leave day must come first");
        assertEquals(LocalDate.parse("2024-01-08"), body.get(1).getDate());
        assertNotNull(body.get(0).getId(), "Reason: the id is what the edit and remove endpoints address");
        assertEquals(0.5, body.get(0).getSickAmount());
        assertNull(body.get(0).getVacationAmount(), "Reason: no vacation was recorded for that date");
        assertEquals(1.0, body.get(1).getVacationAmount());
        assertNull(body.get(1).getSickAmount(), "Reason: no sick leave was recorded for that date");
    }

    @Test
    @FlywayTest
    @DisplayName("an employee may not read another employee's leave days")
    void leaveDaysForOtherUserAsUserTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET,
                String.format(LEAVE_DAYS_URL, 2, OTHER_USER_ID) + "?startDate=" + RANGE_START + "&endDate=" + RANGE_END,
                null, null, null, jwtTokenSeededUser);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "Reason: leave is personal; reading a colleague's stays with administrators");
    }

    @Test
    @FlywayTest
    @DisplayName("GET leave days returns an empty list when none were recorded in the range")
    void leaveDaysEmptyRangeTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET,
                String.format(LEAVE_DAYS_URL, 2, USER_ID) + "?startDate=2030-01-01&endDate=2030-01-31",
                null, 2, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(readList(response, LeaveDayDto.class).isEmpty(),
                "Reason: an empty range is not an error, it is simply no leave");
    }

    @Test
    @FlywayTest
    @DisplayName("edit leave day , admin changes both portions of an existing record - positive")
    void editLeaveDayAsAdminTest() {
        long recordCountBefore = leaveDaysRepository.count();

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, LEAVE_DAY_CHANGE_URL, null, 2,
                createEditLeaveDayDto(SEEDED_LEAVE_SPLIT_ID, null, 1.0, 0.0));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        LeaveDayDto body = readObject(response, LeaveDayDto.class);
        assertEquals(SEEDED_LEAVE_SPLIT_ID, body.getId(),
                "Reason: the response must describe the record that was edited");
        assertEquals(1.0, body.getVacationAmount(), "Reason: the new vacation portion must come back");
        assertNull(body.getSickAmount(), "Reason: a portion set to zero is cleared, not stored as zero");

        // db state validations
        verifyLeaveDayDbState(SEEDED_LEAVE_SPLIT_ID, LocalDate.parse(SEEDED_LEAVE_SPLIT_DAY), 1.0, null);
        assertEquals(recordCountBefore, leaveDaysRepository.count(),
                String.format("Reason: an edit must not change the total count of leave days ,expected:%s ,exist:%s",
                        recordCountBefore, leaveDaysRepository.count()));
    }

    @Test
    @FlywayTest
    @DisplayName("edit leave day , admin moves a record to a free date - positive")
    void editLeaveDayMovesDateTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, LEAVE_DAY_CHANGE_URL, null, 2,
                createEditLeaveDayDto(SEEDED_LEAVE_VACATION_ID, LocalDate.parse(SEEDED_LEAVE_FREE_DAY), null, null));
        assertEquals(HttpStatus.OK, response.getStatusCode());

        LeaveDayDto body = readObject(response, LeaveDayDto.class);
        assertEquals(LocalDate.parse(SEEDED_LEAVE_FREE_DAY), body.getDate(),
                "Reason: the record must have moved to the requested date");
        assertEquals(1.0, body.getVacationAmount(),
                "Reason: portions left out of the request keep their stored value");

        // db state validations
        verifyLeaveDayDbState(SEEDED_LEAVE_VACATION_ID, LocalDate.parse(SEEDED_LEAVE_FREE_DAY), 1.0, null);
    }

    @Test
    @FlywayTest
    @DisplayName("edit leave day , unknown id - negative")
    void editLeaveDayMissingTest() {
        int missingId = 999999;
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, LEAVE_DAY_CHANGE_URL, null, 2,
                createEditLeaveDayDto(missingId, null, 1.0, null));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(String.format(LEAVE_DAY_NOT_FOUND_MSG, missingId), errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("edit leave day , a record owned by another tenant - negative")
    void editLeaveDayOtherTenantTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, LEAVE_DAY_CHANGE_URL, null, 2,
                createEditLeaveDayDto(SEEDED_LEAVE_OTHER_TENANT_ID, null, 0.5, null));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Reason: a record belonging to another tenant must not be reachable");
        assertEquals(String.format(LEAVE_DAY_NOT_FOUND_MSG, SEEDED_LEAVE_OTHER_TENANT_ID), errorMessage(response));
        verifyLeaveDayDbState(SEEDED_LEAVE_OTHER_TENANT_ID, LocalDate.parse("2024-03-07"), 1.0, null);
    }

    @Test
    @FlywayTest
    @DisplayName("edit leave day , portions exceeding a full day - negative")
    void editLeaveDayExceedsFullDayTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, LEAVE_DAY_CHANGE_URL, null, 2,
                createEditLeaveDayDto(SEEDED_LEAVE_SPLIT_ID, null, 1.0, null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Reason: raising vacation to a full day on top of the stored half sick day overflows the date");
        assertEquals(String.format(LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG, SEEDED_LEAVE_SPLIT_DAY, 1.0, 0.5),
                errorMessage(response));
        verifyLeaveDayDbState(SEEDED_LEAVE_SPLIT_ID, LocalDate.parse(SEEDED_LEAVE_SPLIT_DAY), 0.5, 0.5);
    }

    @Test
    @FlywayTest
    @DisplayName("edit leave day , clearing both portions - negative")
    void editLeaveDayLeftEmptyTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, LEAVE_DAY_CHANGE_URL, null, 2,
                createEditLeaveDayDto(SEEDED_LEAVE_SPLIT_ID, null, 0.0, 0.0));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Reason: a record with no leave left on it is a removal, not an edit");
        assertEquals(String.format(LEAVE_DAY_EMPTY_MSG, SEEDED_LEAVE_SPLIT_DAY), errorMessage(response));
        verifyLeaveDayDbState(SEEDED_LEAVE_SPLIT_ID, LocalDate.parse(SEEDED_LEAVE_SPLIT_DAY), 0.5, 0.5);
    }

    @Test
    @FlywayTest
    @DisplayName("edit leave day , moving onto an already recorded date - negative")
    void editLeaveDayOntoRecordedDateTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, LEAVE_DAY_CHANGE_URL, null, 2,
                createEditLeaveDayDto(SEEDED_LEAVE_VACATION_ID, LocalDate.parse(SEEDED_LEAVE_SICK_DAY), null, null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Reason: one record per date, so the clash is reported instead of breaking the unique constraint");
        assertEquals(String.format(LEAVE_DAY_DATE_ALREADY_RECORDED_MSG, SEEDED_LEAVE_SICK_DAY, USER_ID),
                errorMessage(response));
        verifyLeaveDayDbState(SEEDED_LEAVE_VACATION_ID, LocalDate.parse(SEEDED_LEAVE_VACATION_DAY), 1.0, null);
    }

    @Test
    @FlywayTest
    @DisplayName("remove leave day , admin removes an existing record - positive")
    void removeLeaveDayAsAdminTest() {
        long recordCountBefore = leaveDaysRepository.count();

        ResponseEntity<String> response = sendDeleteRequestWithAdmin(HttpMethod.DELETE, LEAVE_DAY_REMOVE_URL,
                SEEDED_LEAVE_SICK_ID, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        LeaveDayDto body = readObject(response, LeaveDayDto.class);
        assertEquals(SEEDED_LEAVE_SICK_ID, body.getId(),
                "Reason: the removed record must come back so the caller can report what went");
        assertEquals(1.0, body.getSickAmount());

        // db state validations
        assertTrue(leaveDaysRepository.findById(SEEDED_LEAVE_SICK_ID).isEmpty(),
                "Reason: the record must be gone from the database");
        assertEquals(recordCountBefore - 1, leaveDaysRepository.count(),
                String.format("Reason: exactly one leave day should have been removed ,expected:%s ,exist:%s",
                        recordCountBefore - 1, leaveDaysRepository.count()));
    }

    @Test
    @FlywayTest
    @DisplayName("remove leave day , unknown id - negative")
    void removeLeaveDayMissingTest() {
        int missingId = 999999;
        long recordCountBefore = leaveDaysRepository.count();

        ResponseEntity<String> response = sendDeleteRequestWithAdmin(HttpMethod.DELETE, LEAVE_DAY_REMOVE_URL,
                missingId, 2, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(String.format(LEAVE_DAY_NOT_FOUND_MSG, missingId), errorMessage(response));
        assertEquals(recordCountBefore, leaveDaysRepository.count(),
                "Reason: nothing should be removed when the record does not exist");
    }

    @Test
    @FlywayTest
    @DisplayName("remove leave day , a record owned by another tenant - negative")
    void removeLeaveDayOtherTenantTest() {
        ResponseEntity<String> response = sendDeleteRequestWithAdmin(HttpMethod.DELETE, LEAVE_DAY_REMOVE_URL,
                SEEDED_LEAVE_OTHER_TENANT_ID, 2, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Reason: a record belonging to another tenant must not be removable");
        assertEquals(String.format(LEAVE_DAY_NOT_FOUND_MSG, SEEDED_LEAVE_OTHER_TENANT_ID), errorMessage(response));
        assertTrue(leaveDaysRepository.findById(SEEDED_LEAVE_OTHER_TENANT_ID).isPresent(),
                "Reason: the other tenant's record must survive untouched");
    }

    @Test
    @FlywayTest
    @DisplayName("add leave days , reporting the same date twice updates the record rather than duplicating it")
    void addLeaveDaysSameDateTwiceTest() {
        LocalDate day = LocalDate.parse("2024-01-15");
        long recordCountBefore = leaveDaysRepository.count();

        ResponseEntity<String> first = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(generateLeaveDayEntryDto(day, 0.5, null))));
        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(recordCountBefore + 1, leaveDaysRepository.count(),
                "Reason: the first report of a date creates its record");

        ResponseEntity<String> second = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(generateLeaveDayEntryDto(day, null, 0.5))));
        assertEquals(HttpStatus.OK, second.getStatusCode());
        assertEquals(recordCountBefore + 1, leaveDaysRepository.count(),
                "Reason: reporting the same date again updates the existing record instead of adding one");

        LeaveDay stored = leaveDaysRepository.findByTenantIdAndIdUserAndLeaveDate(2, USER_ID, day).orElseThrow();
        assertEquals(0.5, stored.getVacationAmount(),
                "Reason: a portion the second report left out must keep its stored value");
        assertEquals(0.5, stored.getSickAmount(), "Reason: the second report's portion must be stored");

        ResponseEntity<String> third = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(generateLeaveDayEntryDto(day, 1.0, null))));
        assertEquals(HttpStatus.BAD_REQUEST, third.getStatusCode(),
                "Reason: the full-day rule is applied to the resulting record, not only to the incoming entry");
        assertEquals(String.format(LEAVE_DAY_AMOUNT_EXCEEDS_FULL_DAY_MSG, day, 1.0, 0.5), errorMessage(third));
    }

    @Test
    @FlywayTest
    @DisplayName("add leave days , an entry reporting neither vacation nor sick time - negative")
    void addLeaveDaysNothingReportedTest() {
        long recordCountBefore = leaveDaysRepository.count();
        LocalDate freeDay = LocalDate.parse("2024-01-17");

        ResponseEntity<String> absent = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(generateLeaveDayEntryDto(freeDay, null, null))));
        assertEquals(HttpStatus.BAD_REQUEST, absent.getStatusCode(),
                "Reason: a leave day has to report something, so omitting both portions is refused");
        assertEquals(String.format(LEAVE_DAY_NOTHING_REPORTED_MSG, freeDay), errorMessage(absent));

        ResponseEntity<String> zeroed = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(generateLeaveDayEntryDto(freeDay, 0.0, 0.0))));
        assertEquals(HttpStatus.BAD_REQUEST, zeroed.getStatusCode(),
                "Reason: zero for both portions reports nothing just as omitting them does");
        assertEquals(String.format(LEAVE_DAY_NOTHING_REPORTED_MSG, freeDay), errorMessage(zeroed));

        // db state validations
        assertEquals(recordCountBefore, leaveDaysRepository.count(),
                "Reason: a refused report must not create any leave day");
    }

    @Test
    @FlywayTest
    @DisplayName("add leave days , a refused entry leaves an already stored record for that date untouched")
    void addLeaveDaysNothingReportedKeepsStoredRecordTest() {
        long recordCountBefore = leaveDaysRepository.count();
        LocalDate day = LocalDate.parse(SEEDED_LEAVE_SPLIT_DAY);

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, ADD_LEAVE_DAYS_URL, USER_ID, 2,
                generateLeaveReportRequestDto(List.of(generateLeaveDayEntryDto(day, 0.0, 0.0))));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Reason: reporting nothing is refused rather than treated as a removal");
        assertEquals(String.format(LEAVE_DAY_NOTHING_REPORTED_MSG, day), errorMessage(response));

        // db state validations
        verifyLeaveDayDbState(SEEDED_LEAVE_SPLIT_ID, day, 0.5, 0.5);
        assertEquals(recordCountBefore, leaveDaysRepository.count(),
                "Reason: removing a leave day is leaveDayRemove's job, never a side effect of a report");
    }
}
