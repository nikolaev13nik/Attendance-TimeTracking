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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceTimeTrackingControllerTest extends BaseApiControllerTest {


    private String range (String base, int idUser) {
        return base + "?startDate=" + RANGE_START + "&finishDate=" + RANGE_END + "&idUser=" + idUser;
    }

    @Test
    @FlywayTest
    @DisplayName("PUT /record/start/{id} as the same user opens a record")
    void startRecordSelfTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, START_URL + USER_ID, null);
        assertEquals (HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject(response, DataTimeDto.class);
        assertNotNull(body.getStart(), "Reason: start must be set");
        assertNull(body.getFinish(), "Reason: finish must be open");
        assertEquals (USER_ID, body.getUser().getIdUser().intValue());
    }

    @Test
    @FlywayTest
    @DisplayName("PUT /record/start/{id} for another user (non-admin) returns 403")
    void startRecordForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, START_URL + OTHER_USER_ID, null);
        assertEquals (HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("PUT /record/start/{id} for a missing user (admin) returns 400")
    void startRecordMissingUserTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, START_URL + "987654", null);
        assertEquals (HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User with login = 987654 not found", errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("PUT /record/finish/{id} closes the open record for today (reused, not inserted)")
    void finishRecordSelfTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.PUT, FINISH_URL + USER_ID, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject(response, DataTimeDto.class);
        assertEquals(SEEDED_TODAY_OPEN_ID, body.getId().intValue(), "Reason: should reuse today's open");
        assertNotNull(body.getFinish(), "Reason: finish must now be set");
        assertNotNull(timeRepository.findById(SEEDED_TODAY_OPEN_ID).orElseThrow().getFinish());
    }

    @Test
    @FlywayTest
    @DisplayName("PUT /record/finish/{id} for another user (non-admin) returns 403")
    void finishRecordForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.PUT, FINISH_URL + OTHER_USER_ID, null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("POST /record as admin edits an existing record")
    void editRecordAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, RECORD_URL, createEditDataTimeUserDto(SEEDED_FINISHED_ID,
                null, LocalDateTime.parse("2024-01-03T18:00:00")));
        assertEquals (HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject (response, DataTimeDto.class);
        assertEquals(LocalDateTime.parse("2024-01-03T18:00:00"), body.getFinish());
        assertEquals(18, timeRepository.findById(SEEDED_FINISHED_ID).orElseThrow().getFinish().getHour());
    }

    @Test
    @FlywayTest
    @DisplayName("POST /record as non-admin returns 403")
    void editRecordForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.POST, RECORD_URL,
                createEditDataTimeUserDto(SEEDED_FINISHED_ID, null, LocalDateTime.parse("2024-01-03T18:00:00")));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("POST /record for a missing record id returns 400")
    void editRecordMissingTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, RECORD_URL,
                createEditDataTimeUserDto(999999, null, LocalDateTime.parse("2024-01-03T18:00:00")));
        assertEquals (HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("idrow is not correct", errorMessage (response));
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
    @DisplayName("GET /record/records?localDate=... as non-admin returns 403")
    void recordsByDayForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, RECORDS_URL + "?localDate=" + SEEDED_DAY, null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records as admin returns the user's records in range")
    void rangeRecordsAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(RANGE_RECORDS_URL, USER_ID), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Integer> ids = readList(response, DataTimeDto.class).stream().map(DataTimeDto::getId ).toList();
        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of (1001, 1002, 1003)));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records as non-admin returns 403")
    void rangeRecordsForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(RANGE_RECORDS_URL, USER_ID), null);
        assertEquals (HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/range/records for a missing user (admin) returns 400")
    void rangeRecordsMissingUserTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(RANGE_RECORDS_URL, 987654), null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User with login = 987654 not found", errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/workdays as admin counts distinct worked days")
    void workdaysAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(WORKDAYS_URL, USER_ID), null);
        assertEquals (HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, readObject(response, Long.class).longValue());
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/workdays as non-admin returns 403")
    void workdaysForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(WORKDAYS_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/hours as admin sums worked minutes")
    void hoursAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(HOURS_URL, USER_ID), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(990L, readObject(response, Long.class).longValue());
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/hours as non-admin returns 403")
    void hoursForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(HOURS_URL, USER_ID), null);
        assertEquals (HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtime as admin sums minutes above 480/day")
    void overtimeAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_URL, USER_ID), null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(30L, readObject(response, Long.class).longValue());
    }

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtime as non-admin returns 403")
    void overtimeForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(OVERTIME_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
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
    @DisplayName("GET /record/check as non-admin returns 403")
    void checkForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.GET, range(CHECK_URL, USER_ID), null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE /record/remove/{id} as admin removes the record")
    void removeRecordAsAdminTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.DELETE, REMOVE_URL + SEEDED_FINISHED_ID, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DataTimeDto body = readObject(response, DataTimeDto.class);
        assertEquals(SEEDED_FINISHED_ID, body.getId().intValue());
        assertFalse(timeRepository.existsById(SEEDED_FINISHED_ID));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE /record/remove/{id} as non-admin returns 403")
    void removeRecordForbiddenTest() {
        ResponseEntity<String> response = sendRequestWithUserRole(HttpMethod.DELETE, REMOVE_URL + SEEDED_FINISHED_ID, null);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ACCESS_DENIED, errorMessage(response));
        assertTrue(timeRepository.existsById(SEEDED_FINISHED_ID));
    }

    @Test
    @FlywayTest
    @DisplayName("DELETE /record/remove/{id} for a missing record returns 400")
    void removeRecordMissingTest() {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.DELETE, REMOVE_URL + "999999", null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("idrow is not correct", errorMessage(response));
    }
}