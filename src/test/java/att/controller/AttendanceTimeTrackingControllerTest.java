package att.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import att.dto.DataTimeDto;
import att.dto.SessionDataDto;

import static att.exceptions.ErrorConstants.ATTENDANCE_NOT_FOUND_MSG;
import static att.exceptions.ErrorConstants.OPEN_CLOSE_DATE_MISSING_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        OffsetDateTime openDate = OffsetDateTime.now();
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
    @DisplayName("start record with wrong worker date returns 400 -negative")
    void openWorkSession_wrongWorkerDateTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime openDate = OffsetDateTime.now();
        LocalDate inputWrongWorkDate = LocalDate.parse("2026-08-03");
        SessionDataDto task = generateSessionDataDto(inputWrongWorkDate, openDate, null);
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, OPEN_URL, 987654, tenant,
                task);
        assertEquals (HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(errorMessage(response).contains(
                String.format("Input work date:%s expects to be as", inputWrongWorkDate)));
        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, sessionAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("close work session, closes the open record for today (reused, not inserted), then insert - positive")
    void closeWorkSessionTest_positive() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        int tenant = 2;
        OffsetDateTime closeSessionDate = OffsetDateTime.now();
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
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(WORKDAYS_URL, USER_ID), null,
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
    @DisplayName("GET get total hours as admin sums worked minutes")
    void getTotalHoursAsAdminTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(HOURS_URL, USER_ID), null,
                2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(990L, readObject(response, Long.class).longValue());

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtime as admin sums minutes above 480/day")
    void overtimeAsAdminTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_URL, USER_ID), null,
                2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(30L, readObject(response, Long.class).longValue());

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
}