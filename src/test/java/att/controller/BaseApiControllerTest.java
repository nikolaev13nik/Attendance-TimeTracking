package att.controller;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import att.dao.SessionAttendanceTimeRepository;
import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.dto.SessionDataDto;
import att.model.DataTime;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestExecutionListeners(
        listeners = { FlywayTestExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public abstract class BaseApiControllerTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;
    protected static final Integer USER_ID = 2;
    protected static final Integer OTHER_USER_ID = 3;
    protected static final String ACCESS_DENIED = "Access Denied";
    protected static final String BASE_SUFFIX_URL = "/attendance";
    protected static final String TENANT_ID_URL = "/tenant/%s";
    protected static final String EDIT_URL = BASE_SUFFIX_URL + "/sessionChange" + TENANT_ID_URL;

    protected static final String OPEN_URL = BASE_SUFFIX_URL + "/openSession/tenant/%s/userId/%s";
    protected static final String CLOSE_URL = BASE_SUFFIX_URL + "/closeSession/tenant/%s/userId/%s";

    protected static final String RECORDS_URL = BASE_SUFFIX_URL + "/sessions" + TENANT_ID_URL;
    protected static final String RANGE_RECORDS_URL = RECORDS_URL;
    protected static final String WORKDAYS_URL = BASE_SUFFIX_URL + "/workdays" + TENANT_ID_URL;
    protected static final String HOURS_URL = BASE_SUFFIX_URL + "/hours" + TENANT_ID_URL;
    protected static final String OVERTIME_URL = BASE_SUFFIX_URL + "/overtime" + TENANT_ID_URL;
    protected static final String CHECK_URL = BASE_SUFFIX_URL + "/check" + TENANT_ID_URL + "/user/%s";
    protected static final String REMOVE_URL = BASE_SUFFIX_URL + "/sessionRemove" + TENANT_ID_URL + "/session/%s";

    @Autowired
    protected SessionAttendanceTimeRepository sessionAttendanceTimeRepository;

    protected static final String RANGE_START = "2024-01-01";
    protected static final String RANGE_END = "2024-01-31";

    protected static final String SEEDED_DAY = "2024-01-02";
    protected static final int SEEDED_TODAY_OPEN_ID = 1005;
    protected static final int SEEDED_FINISHED_ID = 1002;
    protected static final int SEEDED_OPEN_ID = 1003;

    private final RestTemplate rest = createRestTemplate();

    protected String jwtTokenAdministrator = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkiLCJleHAiOjIxMDEwMzI4MzEsImlhdCI6MTc4NTY3MjgzMSwiYXV0aG9yaXRpZXMiOlsiQURNSU4iLCJBRE1JTklTVFJBVE9SIiwiTU9ERVJBVE9SIiwiVVNFUiIsIkZBQ1RPUl9QQVNTV09SRCJdfQ.7v9mluTswU2F7GRdG5xKQxStTGn5bEy_5Dn74FYqShU";
    protected String jwtTokenUser = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJleHAiOjIxMDEwMzI3MDIsImlhdCI6MTc4NTY3MjcwMiwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIkZBQ1RPUl9QQVNTV09SRCJdfQ.x_Jobfo63CL4eUxU6lBO11SyMG7ZdQeO5Z3S5wyjbLY";
    protected String jwtTokenUserTenant_123 = "eyJhbGciOiJIUzI1NiJ9" +
            ".eyJ0ZW5hbnRJZCI6MTIzLCJzdWIiOiIxMjMiLCJleHAiOjIxMDE3MDkzNDAsImlhdCI6MTc4NjM0OTM0MCwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIkZBQ1RPUl9QQVNTV09SRCJdfQ.zSHpMa_RqUSFByzlk5MxchZuInlxZihHJbPfD9gNodw";
    private static RestTemplate createRestTemplate() {
            RestTemplate rt = new RestTemplate();
            rt.setErrorHandler(new DefaultResponseErrorHandler() {
                @Override
                public boolean hasError(ClientHttpResponse response){
                    return false;
                }
            });
            return rt;
    }

    private HttpHeaders getHeaders(String jwtTokenAdministrator) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenAdministrator);
        return headers;
    }

    protected ResponseEntity<String> sendRequestWithAdmin(HttpMethod method, String path,
                                                          Integer userId, int tenantId, Object requestBody) {
        return sendRequest(method, buildUrl(path, userId, tenantId), requestBody, getHeaders(jwtTokenAdministrator));
    }

    protected ResponseEntity<String> sendDeleteRequestWithAdmin(HttpMethod method, String path,
                                                                Integer sessionId, int tenantId, Object requestBody) {

        return sendRequest(method, String.format(path, tenantId, sessionId), requestBody,
                getHeaders(jwtTokenAdministrator));
    }

    private String buildUrl(String path, Integer userId, Integer tenantId) {
        if (Objects.isNull(userId)) {
            return String.format(path, tenantId);
        }
        return String.format(path, tenantId, userId);
    }

    protected ResponseEntity<String> sendRequestWithUserRole(HttpMethod method, String path,
                                                             Integer userId, Integer tenantId,
                                                             Object requestBody) {
        return sendRequest(method, buildUrl(path, userId, tenantId), requestBody, getHeaders(jwtTokenUser));
    }

    protected ResponseEntity<String> sendRequestWithUserRole(HttpMethod method, String path,
                                                             Integer userId, Integer tenantId,
                                                             Object requestBody, String jwtToken) {
        return sendRequest(method, buildUrl(path, userId, tenantId), requestBody, getHeaders(jwtToken));
    }

    private ResponseEntity<String> sendRequest(HttpMethod method, String path, Object requestBody, HttpHeaders headers) {
        Object payload = (requestBody == null || requestBody instanceof String) ? requestBody : objectMapper.writeValueAsString(requestBody);
        ResponseEntity<String> response;

        response = rest.exchange(url(path), method, new HttpEntity<>(payload, headers),
                String.class);
        if (response.getStatusCode().isError()) {
            System.out.println("STATUS: " + response.getStatusCode());
            System.out.println(response.getBody());
        }
        return response;
    }

    protected String url(String path) {
        return String.format("http://localhost:%s%s", port, path);
    }


    /** Deserialize the response body into a single Object */
    protected <T> T readObject(ResponseEntity<String> response, Class<T> clazz) {
        try {
            return objectMapper.readValue(response.getBody(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON object", e);
        }
    }

    /** Deserialize the response body into a List of the given element type */
    protected <T> List<T> readList(ResponseEntity<String> response, Class<T> elementType) {
        try {
            return objectMapper.readValue(
                    response.getBody(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON list", e);
        }
    }

    /** Parse the raw string response body to a Jackson JsonNode */
    protected JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body to JSON tree", e);
        }
    }

    /** Retrieve message field from a structured error response */
    protected String errorMessage(ResponseEntity<String> response) {
        try {
            JsonNode node = json(response);
            if (node.has("message")) {
                return node.get("message").asText();
            }
            return response.getBody();
        } catch (Exception e) {
            return response.getBody();
        }
    }

    protected EditDataTimeUserDto createEditDataTimeUserDto(Integer id, OffsetDateTime start, OffsetDateTime finish) {
        return EditDataTimeUserDto.builder()
                .id(id)
                .openSessionDate(start)
                .closeSessionDate(finish)
                .build();
    }

    protected void verifyRecordStartEndApi(Integer rawId, Integer userId, OffsetDateTime startTimeDate,
                                           OffsetDateTime endTimeDate) {
        Optional<DataTime> opSession = sessionAttendanceTimeRepository.findById(rawId);
        assertTrue(opSession.isPresent());
        assertEquals(userId, opSession.get().getIdUser());
        ofNullable(startTimeDate).ifPresent(
                startTime -> assertEquals(startTime.toInstant(), opSession.get().getOpenSessionDate().toInstant()));
        ofNullable(endTimeDate).ifPresent(
                endTime -> assertEquals(endTime.toInstant(), opSession.get().getCloseSessionDate().toInstant()));
    }

    protected void verifyRespondedBodyOpenCloseSessionApi(DataTimeDto body, Integer userId,
                                                          int tenant, OffsetDateTime startTime,
                                                          OffsetDateTime endTime,
                                                          Integer expectedRecordId) {
        ofNullable(expectedRecordId).ifPresent(
                recId -> assertEquals(recId, body.getId(), "Reason: should reuse today's open"));
        assertNotNull(body.getUserId());
        assertNotNull(body.getId());
        assertEquals(userId, body.getUserId());
        ofNullable(startTime).ifPresent(
                expectedStartTime -> assertEquals(expectedStartTime.toInstant(),
                        body.getOpenSessionDate().toInstant(), "Reason: start must be set"));
        ofNullable(endTime).ifPresent(expectedEndTime -> assertEquals(expectedEndTime.toInstant(),
                body.getCloseSessionDate().toInstant(), "Reason: finish must be set"));
        assertEquals(tenant, body.getTenantId());
    }

    protected String range(String base, int idUser) {
        return base + "?startDate=" + RANGE_START + "&endDate=" + RANGE_END + "&idUser=" + idUser;
    }

    protected String range(String base) {
        return base + "?startDate=" + RANGE_START + "&endDate=" + RANGE_END;
    }

    protected SessionDataDto generateSessionDataDto(LocalDate workDate, OffsetDateTime openDate,
                                                    OffsetDateTime closeDate) {
        SessionDataDto task = new SessionDataDto();
        task.setOpenSessionDate(openDate);
        task.setCloseSessionDate(closeDate);
        task.setWorkDate(workDate);
        return task;
    }
}
