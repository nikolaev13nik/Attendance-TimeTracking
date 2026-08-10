package att.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import att.dto.SessionDataDto;
import att.model.DataTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SecurityControlTest extends BaseApiControllerTest {

    @Test
    @FlywayTest
    @DisplayName("start record for another user (non-admin) - negative")
    void startRecordForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        OffsetDateTime openDate = OffsetDateTime.now();
        SessionDataDto task = generateSessionDataDto(LocalDate.now(), openDate, null);
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, OPEN_URL,
                OTHER_USER_ID, 123, task, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        // db state validations
        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("finish record for another user (non-admin) returns 403")
    void finishRecordForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        OffsetDateTime closeSessionDate = OffsetDateTime.now();
        LocalDate inputWorkDate = closeSessionDate.toLocalDate();
        SessionDataDto task = generateSessionDataDto(inputWorkDate, null, closeSessionDate);
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, CLOSE_URL,
                2, 123, task, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }


    @Test
    @FlywayTest
    @DisplayName("edit record as non-admin returns 403")
    void editRecordForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        OffsetDateTime newFinishTime = OffsetDateTime.parse("2024-01-03T18:00:00Z");
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.POST,
                EDIT_URL, 2, 123,
                createEditDataTimeUserDto(SEEDED_FINISHED_ID, null, newFinishTime), jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
        DataTime dataTime = sessionAttendanceTimeRepository.findById(SEEDED_FINISHED_ID).orElseThrow();
        assertNotEquals(dataTime.getCloseSessionDate(), newFinishTime);
    }


    @Test
    @FlywayTest
    @DisplayName("GET /record/records?localDate=... as non-admin returns 403")
    void recordsByDayForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(RANGE_RECORDS_URL, 123456),
                3, 123, null, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records as non-admin returns 403")
    void rangeRecordsForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET,
                range(RANGE_RECORDS_URL, USER_ID), 1, 123, null, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/workdays as non-admin returns 403")
    void workdaysForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET,
                range(WORKDAYS_URL, USER_ID), 2, 123, null, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/hours as non-admin returns 403")
    void hoursForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(HOURS_URL
                , USER_ID), null, 123, null, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtime as non-admin returns 403")
    void overtimeForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET,
                range(OVERTIME_URL, USER_ID), 1, 123, null, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }


    @Test
    @FlywayTest
    @DisplayName("GET /record/check as non-admin returns 403")
    void checkForbiddenTest() {

        long recordCountBefore = sessionAttendanceTimeRepository.count();
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(CHECK_URL), USER_ID,
                123, null, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE /record/remove/{id} as non-admin returns 403")
    void removeRecordForbiddenTest() {
        long recordCountBefore = sessionAttendanceTimeRepository.count();

        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.DELETE, REMOVE_URL,
                SEEDED_FINISHED_ID, 123, null, jwtTokenUserTenant_123);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
        assertTrue(sessionAttendanceTimeRepository.existsById(SEEDED_FINISHED_ID));

        long countAfter = sessionAttendanceTimeRepository.count();
        assertEquals(recordCountBefore, countAfter,
                String.format("Reason: total count of attendance record should not be changed ,expected:%s ,exist:%s",
                        recordCountBefore, countAfter));
    }

}
