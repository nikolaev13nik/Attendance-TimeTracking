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

@TestPropertySource(properties = "att.overtime.daily-threshold=8.4")
class OvertimeThresholdConfigurationTest extends BaseApiControllerTest {

    @Test
    @FlywayTest
    @DisplayName("GET /record/overtimeMinutes reads the configured threshold as H.MM clock time")
    void overtimeThresholdIsHoursDotMinutesTest() {
        ResponseEntity<String> closeBlocking = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL, null, 2,
                createEditDataTimeUserDto(SEEDED_OPEN_ID, null, OffsetDateTime.parse("2024-01-04T18:00:00Z")));
        assertEquals(HttpStatus.OK, closeBlocking.getStatusCode());

        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_MINUTES_URL, USER_ID),
                null, 2, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(20L, readObject(response, Long.class).longValue(),
                "Reason: 8.4 means 8h40m (520 min), so only the 9h on 2024-01-04 exceeds it, by 20 min; "
                        + "as decimal hours (504 min) the 8h30m days would add overtime too");
    }
}
