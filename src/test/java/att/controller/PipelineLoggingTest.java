package att.controller;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static att.exceptions.ErrorConstants.INCOMPLETE_SESSIONS_MSG;
import static att.exceptions.ErrorConstants.LEAVE_DAY_NOT_FOUND_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class PipelineLoggingTest extends BaseApiControllerTest {

    private static final int TENANT = 2;

    private static String strategyLine(CapturedOutput output, String strategy, String message) {
        return output.getOut().lines()
                .filter(line -> line.contains(strategy) && line.contains(message))
                .findFirst()
                .orElse("");
    }

    @Test
    @FlywayTest
    @DisplayName("A strategy that changes data writes one INFO 'completed' line")
    void completedChangeIsLoggedTest(CapturedOutput output) {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.POST, EDIT_URL, null, TENANT,
                createEditDataTimeUserDto(SEEDED_FINISHED_ID, null, OffsetDateTime.parse("2024-01-02T18:00:00Z")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String line = strategyLine(output, "SessionChangeService", "completed");
        assertTrue(line.contains(" INFO "), "Reason: a change is logged at INFO, line: " + line);
    }

    @Test
    @FlywayTest
    @DisplayName("A business rejection writes one WARN line with the reason the caller got")
    void businessRejectionIsLoggedTest(CapturedOutput output) {
        ResponseEntity<String> response = sendDeleteRequestWithAdmin(HttpMethod.DELETE, LEAVE_DAY_REMOVE_URL,
                SEEDED_LEAVE_OTHER_TENANT_ID, TENANT, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        String line = strategyLine(output, "LeaveDayRemoveService",
                "rejected: " + String.format(LEAVE_DAY_NOT_FOUND_MSG, SEEDED_LEAVE_OTHER_TENANT_ID));
        assertTrue(line.contains(" WARN "), "Reason: a business rejection is logged at WARN, line: " + line);
    }

    @Test
    @FlywayTest
    @DisplayName("A rejected read names the target user it was asked about")
    void rejectedReadNamesTargetUserTest(CapturedOutput output) {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(OVERTIME_MINUTES_URL, USER_ID),
                null, TENANT, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String line = strategyLine(output, "GetOvertimeMinutesBetweenService", "rejected: " + INCOMPLETE_SESSIONS_MSG);
        assertTrue(line.contains("target=" + USER_ID + "]"), "Reason: the line names the user asked about, line: " + line);
    }

    @Test
    @FlywayTest
    @DisplayName("A read strategy also writes its INFO 'completed' line with the target user")
    void completedReadIsLoggedTest(CapturedOutput output) {
        ResponseEntity<String> response = sendRequestWithAdmin(HttpMethod.GET, range(RECORDS_URL, USER_ID), null,
                TENANT, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String line = strategyLine(output, "GetRecordsByRangeService", "completed");
        assertTrue(line.contains(" INFO ") && line.contains("target=" + USER_ID + "]"),
                "Reason: every strategy run is logged at INFO, line: " + line);
    }
}
