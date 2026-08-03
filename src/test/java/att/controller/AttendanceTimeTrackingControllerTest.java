package att.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import att.dto.DataTimeDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceTimeTrackingControllerTest extends BaseApiControllerTest {



    @Test
    @FlywayTest
    @DisplayName("start record ,the same user opens a record - positive")
    void startRecordSelfTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, START_URL + USER_ID, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // response validation
        DataTimeDto body = readObject(response, DataTimeDto.class);
        verifyRecordStartEndResponseApi(body, USER_ID, true, false, null);
        // db state validations
        verifyRecordStartEndApi(body.getId(), USER_ID, body.getStart(), null);
        assertEquals(recordCountBefore + 1, userAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record is to be increased abd expect to be as:%s", recordCountBefore + 1));
    }

    @Test
    @FlywayTest
    @DisplayName("start record for a missing user (admin) returns 400 -negative")
    void startRecordMissingUserTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, START_URL + "987654", null);
        assertEquals (HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User with login = 987654 not found", errorMessage(response));
        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, userAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("finish record, closes the open record for today (reused, not inserted) - positive")
    void finishRecordTest_positive() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, FINISH_URL + USER_ID, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // response validation
        DataTimeDto body = readObject(response, DataTimeDto.class);
        verifyRecordStartEndResponseApi(body, USER_ID, true, true, SEEDED_TODAY_OPEN_ID);
        // db state validations
        verifyRecordStartEndApi(body.getId(), USER_ID, body.getStart(), body.getFinish());
        assertEquals(recordCountBefore, userAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed,and expect to be as:%s", recordCountBefore));
    }

    @Test
    @FlywayTest
    @DisplayName("edit record , admin edits an existing record - positive")
    void editRecordAsAdminTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        LocalDateTime newFinishTime = LocalDateTime.parse("2024-01-03T18:00:00");
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, RECORD_URL, createEditDataTimeUserDto(SEEDED_FINISHED_ID,
                null, newFinishTime));
        assertEquals (HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject (response, DataTimeDto.class);
        assertEquals(newFinishTime, body.getFinish());

        // db state validations
        assertEquals(18, userAttendanceTimeRepository.findById(SEEDED_FINISHED_ID).orElseThrow().getFinish().getHour());
        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, userAttendanceTimeRepository.count(),
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("edit record for a missing record id returns 400")
    void editRecordMissingTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        LocalDateTime newFinishTime = LocalDateTime.parse("2024-01-03T18:00:00");
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, RECORD_URL,
                createEditDataTimeUserDto(999999, null, newFinishTime));
        assertEquals (HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("id row is not correct", errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/records?localDate=... as admin returns that day's records")
    void recordsByDayAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, RECORDS_URL + "?localDate=" + SEEDED_DAY, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId).toList();
        assertTrue(ids.contains(1001));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records as admin returns the user's records in range")
    void rangeRecordsAsAdminTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(RANGE_RECORDS_URL, USER_ID), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId ).toList();
        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of (1001, 1002, 1003)));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records for a missing user (admin) returns 400")
    void rangeRecordsMissingUserTest() {
        String fakeUserId = "987654";
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(RANGE_RECORDS_URL, Integer.parseInt(fakeUserId)), null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(String.format("User with login = %s not found", fakeUserId), errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/workdays as admin counts distinct worked days")
    void workdaysAsAdminTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(WORKDAYS_URL, USER_ID), null);
        assertEquals (HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, readObject(response, Long.class).longValue());

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/hours as admin sums worked minutes")
    void hoursAsAdminTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(HOURS_URL, USER_ID), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(990L, readObject(response, Long.class).longValue());

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtime as admin sums minutes above 480/day")
    void overtimeAsAdminTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_URL, USER_ID), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(30L, readObject(response, Long.class).longValue());

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/check as admin returns rows with an open start/finish")
    void checkAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(CHECK_URL, USER_ID), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId).toList();
        assertEquals(1, ids.size());
        assertTrue(ids.contains(SEEDED_OPEN_ID));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE /record/remove/{id} as admin removes the record")
    void removeRecordAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.DELETE, REMOVE_URL + SEEDED_FINISHED_ID, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject(response, DataTimeDto.class);
        assertEquals(SEEDED_FINISHED_ID, body.getId().intValue());
        assertFalse(userAttendanceTimeRepository.existsById(SEEDED_FINISHED_ID));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE /record/remove/{id} for a missing record returns 400")
    void removeRecordMissingTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.DELETE, REMOVE_URL + "999999", null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("id row is not correct", errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }
}