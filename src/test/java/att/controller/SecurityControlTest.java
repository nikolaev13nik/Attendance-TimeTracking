package att.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import att.model.DataTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SecurityControlTest extends BaseApiControllerTest {

    @Test
    @FlywayTest
    @DisplayName("start record for another user (non-admin) - negative")
    void startRecordForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, START_URL + OTHER_USER_ID, null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        // db state validations
        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("finish record for another user (non-admin) returns 403")
    void finishRecordForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, FINISH_URL + OTHER_USER_ID, null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }


    @Test
    @FlywayTest
    @DisplayName("edit record as non-admin returns 403")
    void editRecordForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        LocalDateTime newFinishTime = LocalDateTime.parse("2024-01-03T18:00:00");
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.POST, RECORD_URL,
                createEditDataTimeUserDto(SEEDED_FINISHED_ID, null, newFinishTime));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
        DataTime dataTime = userAttendanceTimeRepository.findById(SEEDED_FINISHED_ID).orElseThrow();
        assertNotEquals(dataTime.getFinish(), newFinishTime);
    }


    @Test
    @FlywayTest
    @DisplayName("GET /record/records?localDate=... as non-admin returns 403")
    void recordsByDayForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, RECORDS_URL + "?localDate=" + SEEDED_DAY, null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records as non-admin returns 403")
    void rangeRecordsForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(RANGE_RECORDS_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/workdays as non-admin returns 403")
    void workdaysForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(WORKDAYS_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/hours as non-admin returns 403")
    void hoursForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(HOURS_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtime as non-admin returns 403")
    void overtimeForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(OVERTIME_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }


    @Test
    @FlywayTest
    @DisplayName("GET /record/check as non-admin returns 403")
    void checkForbiddenTest() {

        long recordCountBefore = userAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(CHECK_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE /record/remove/{id} as non-admin returns 403")
    void removeRecordForbiddenTest() {
        long recordCountBefore = userAttendanceTimeRepository.count();

        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.DELETE, REMOVE_URL + SEEDED_FINISHED_ID, null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
        assertTrue(userAttendanceTimeRepository.existsById(SEEDED_FINISHED_ID));

        long countAfter = userAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }
}
