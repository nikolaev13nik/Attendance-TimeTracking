package att.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = "att.overtime.daily-threshold=8.75")
class OvertimeThresholdFallbackTest extends BaseApiControllerTest {

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtimeMinutes falls back to 8 hours when the configured threshold is unreadable")
    void unreadableThresholdFallsBackToEightHoursTest() {
        ResponseEntity<String> closeBlocking = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL, null, 2,
                createEditDataTimeUserDto(SEEDED_OPEN_ID, null, OffsetDateTime.parse("2024-01-04T17:30:00Z")));
        assertEquals(HttpStatus.OK, closeBlocking.getStatusCode());

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_MINUTES_URL, USER_ID),
                null, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(60L, readObject(response, Long.class).longValue(),
                "Reason: 8.75 has 75 minutes, so it is ignored and the 8h default leaves 30 + 30 overtime minutes");
    }
}
