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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import att.client.accounting.api.AccountApi;
import att.client.accounting.dto.UserProfileDto;
import att.dao.LeaveDaysRepository;
import att.dao.MonthStatisticRepository;
import att.dao.SessionAttendanceTimeRepository;
import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.dto.EditLeaveDayDto;
import att.dto.LeaveDayEntryDto;
import att.dto.LeaveReportRequestDto;
import att.dto.MonthlyUserStatisticInfoDto;
import att.dto.SessionDataDto;
import att.messaging.producer.StatisticEventProducer;
import att.model.DataTime;
import att.model.LeaveDay;
import att.model.MonthStatistic;
import att.model.MonthStatisticKey;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
    protected static final String MINUTES_URL = BASE_SUFFIX_URL + "/minutes" + TENANT_ID_URL;
    protected static final String OVERTIME_MINUTES_URL = BASE_SUFFIX_URL + "/overtimeMinutes" + TENANT_ID_URL;
    protected static final String CHECK_URL = BASE_SUFFIX_URL + "/check" + TENANT_ID_URL + "/user/%s";
    protected static final String REMOVE_URL = BASE_SUFFIX_URL + "/sessionRemove" + TENANT_ID_URL + "/session/%s";
    protected static final String ADD_LEAVE_DAYS_URL = BASE_SUFFIX_URL + "/addLeaveDays/tenant/%s/userId/%s";
    protected static final String LEAVE_DAYS_URL = BASE_SUFFIX_URL + "/leaveDays/tenant/%s/userId/%s";
    protected static final String LEAVE_DAY_CHANGE_URL = BASE_SUFFIX_URL + "/leaveDayChange" + TENANT_ID_URL;
    protected static final String LEAVE_DAY_REMOVE_URL =
            BASE_SUFFIX_URL + "/leaveDayRemove" + TENANT_ID_URL + "/leaveDay/%s";
    protected static final String STATISTIC_URL = BASE_SUFFIX_URL + "/statistic" + TENANT_ID_URL;

    protected static final Integer SEEDED_STAT_USER_ID = 7;

    @Autowired
    protected SessionAttendanceTimeRepository sessionAttendanceTimeRepository;

    @Autowired
    protected LeaveDaysRepository leaveDaysRepository;

    @Autowired
    protected MonthStatisticRepository monthStatisticRepository;

    protected static final String RANGE_START = "2024-01-01";
    protected static final String RANGE_END = "2024-01-31";

    protected static final String SEEDED_DAY = "2024-01-02";
    protected static final int SEEDED_TODAY_OPEN_ID = 1005;
    protected static final int SEEDED_FINISHED_ID = 1002;
    protected static final int SEEDED_OPEN_ID = 1003;

    /**
     * Leave-day fixtures from {@code V4__att_user_leave_days_setup.sql}: a vacation-only day, a
     * half-and-half day and a sick-only day for the seeded employee, plus one row owned by tenant 1.
     */
    protected static final int SEEDED_LEAVE_VACATION_ID = 2001;
    protected static final int SEEDED_LEAVE_SPLIT_ID = 2002;
    protected static final int SEEDED_LEAVE_SICK_ID = 2003;
    protected static final int SEEDED_LEAVE_OTHER_TENANT_ID = 2004;
    protected static final String SEEDED_LEAVE_VACATION_DAY = "2024-03-04";
    protected static final String SEEDED_LEAVE_SPLIT_DAY = "2024-03-05";
    protected static final String SEEDED_LEAVE_SICK_DAY = "2024-03-06";
    protected static final String SEEDED_LEAVE_FREE_DAY = "2024-03-20";
    protected static final String LEAVE_RANGE_START = "2024-03-01";
    protected static final String LEAVE_RANGE_END = "2024-03-31";
    protected static final Integer SEEDED_STAT_OTHER_USER_ID = 8;
    protected static final int SEEDED_STAT_OTHER_TENANT_ID = 3;
    protected static final String SEEDED_STAT_TARGET_MONTH = "2024-01";
    protected static final String SEEDED_STAT_PREVIOUS_MONTH = "2023-12";
    protected static final String SEEDED_STAT_OLDEST_EXCLUDED_MONTH = "2023-06";
    protected static final String SEEDED_STAT_FUTURE_MONTH = "2024-02";
    @MockitoBean
    protected StatisticEventProducer statisticEventProducer;

    /**
     * The {@code sub} claim carried by {@link #jwtTokenAdministrator} - the report requester in these tests.
     */
    protected static final Integer ADMIN_SUB_ID = 123456789;
    protected static final String REPORT_FIRST_NAME = "John";
    protected static final String REPORT_LAST_NAME = "Doe";
    protected static final String REPORT_EMAIL = "john.doe@example.com";
    protected static final String TARGET_FIRST_NAME_PREFIX = "Employee";
    protected static final String TARGET_LAST_NAME = "Smith";

    @MockitoBean
    protected AccountApi accountApi;

    /**
     * Makes Attendance-Accounting answer the report-requester lookup the statistic pipeline performs.
     */
    protected void stubReportRequester(int tenantId) {
        when(accountApi.getUser(tenantId, ADMIN_SUB_ID)).thenReturn(new UserProfileDto()
                .idUser(ADMIN_SUB_ID)
                .tenantId(tenantId)
                .firstName(REPORT_FIRST_NAME)
                .lastName(REPORT_LAST_NAME)
                .email(REPORT_EMAIL));
    }

    /**
     * Makes Attendance-Accounting answer the reported-user lookups: {@code getUser} for a single-user report and
     * {@code getAllUsers} for a tenant-wide one, both naming {@link #USER_ID} and {@link #OTHER_USER_ID}.
     */
    protected void stubTargetUserProfiles(int tenantId) {
        when(accountApi.getUser(tenantId, USER_ID)).thenReturn(targetProfile(tenantId, USER_ID));
        when(accountApi.getUser(tenantId, OTHER_USER_ID)).thenReturn(targetProfile(tenantId, OTHER_USER_ID));
        when(accountApi.getAllUsers(tenantId))
                .thenReturn(List.of(targetProfile(tenantId, USER_ID), targetProfile(tenantId, OTHER_USER_ID)));
    }

    private UserProfileDto targetProfile(int tenantId, Integer idUser) {
        return new UserProfileDto()
                .idUser(idUser)
                .tenantId(tenantId)
                .firstName(TARGET_FIRST_NAME_PREFIX + idUser)
                .lastName(TARGET_LAST_NAME);
    }

    protected String expectedTargetUserName(Integer idUser) {
        return "%s%d %s".formatted(TARGET_FIRST_NAME_PREFIX, idUser, TARGET_LAST_NAME);
    }

    protected void assertReportRequester(String reportUserName, String reportUserLastName, String reportEmail) {
        assertEquals(REPORT_FIRST_NAME, reportUserName,
                "Reason: the requester's first name resolved from Accounting must travel on the event");
        assertEquals(REPORT_LAST_NAME, reportUserLastName,
                "Reason: the requester's last name resolved from Accounting must travel on the event");
        assertEquals(REPORT_EMAIL, reportEmail,
                "Reason: the AI service can only notify the requester if the email travels on the event");
    }


    private final RestTemplate rest = createRestTemplate();

    protected String jwtTokenAdministrator = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkiLCJleHAiOjIxMDEwMzI4MzEsImlhdCI6MTc4NTY3MjgzMSwiYXV0aG9yaXRpZXMiOlsiQURNSU4iLCJBRE1JTklTVFJBVE9SIiwiTU9ERVJBVE9SIiwiVVNFUiIsIkZBQ1RPUl9QQVNTV09SRCJdfQ.7v9mluTswU2F7GRdG5xKQxStTGn5bEy_5Dn74FYqShU";
    protected String jwtTokenUser = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJleHAiOjIxMDEwMzI3MDIsImlhdCI6MTc4NTY3MjcwMiwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIkZBQ1RPUl9QQVNTV09SRCJdfQ.x_Jobfo63CL4eUxU6lBO11SyMG7ZdQeO5Z3S5wyjbLY";
    /**
     * The seeded employee: sub 2 inside tenant 2, which is where the session fixtures live.
     */
    protected String jwtTokenSeededUser = "eyJhbGciOiJIUzI1NiJ9.eyJ0ZW5hbnRJZCI6Miwic3ViIjoiMiIsImV4cCI6MjEwMTcwOTM0MCwiaWF0IjoxNzg2MzQ5MzQwLCJhdXRob3JpdGllcyI6WyJVU0VSIiwiRkFDVE9SX1BBU1NXT1JEIl19.i_M1miaOQhb2_VwYbyRJD9N-5sokFKEO-SreC0LX7MY";

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

    protected void verifyMonthStatisticDbState(MonthlyUserStatisticInfoDto stat, int tenantId) {
        MonthStatisticKey key = new MonthStatisticKey(tenantId, stat.getUserId(), stat.getYearMonth().atDay(1));
        Optional<MonthStatistic> saved = monthStatisticRepository.findById(key);
        assertTrue(saved.isPresent(),
                String.format("Reason: expected a persisted MonthStatistic row for tenant:%s user:%s month:%s",
                        tenantId, stat.getUserId(), stat.getYearMonth()));
        MonthStatistic entity = saved.get();
        assertEquals(stat.getWorkDays(), entity.getWorkDays(),
                "Reason: db workDays should match the API response");
        assertEquals(stat.getTotalWorkHours(), entity.getTotalWorkHours(),
                "Reason: db totalWorkHours should match the API response");
        assertEquals(stat.getOvertimeHours(), entity.getOvertimeHours(),
                "Reason: db overtimeHours should match the API response");
        assertEquals(stat.getVacationDays(), entity.getVacationDays(),
                "Reason: db vacationDays should match the API response");
        assertEquals(stat.getSickDays(), entity.getSickDays(),
                "Reason: db sickDays should match the API response");
    }

    protected String range(String base, int idUser) {
        return base + "?startDate=" + RANGE_START + "&endDate=" + RANGE_END + "&idUser=" + idUser;
    }

    protected String range(String base) {
        return base + "?startDate=" + RANGE_START + "&endDate=" + RANGE_END;
    }

    protected String statistic(String base, String targetMonth) {
        return base + "?targetMonth=" + targetMonth;
    }

    protected String statistic(String base, String targetMonth, int idUser) {
        return statistic(base, targetMonth) + "&idUser=" + idUser;
    }

    protected String withReport(String statisticUrl, boolean report) {
        return statisticUrl + "&report=" + report;
    }

    protected SessionDataDto generateSessionDataDto(LocalDate workDate, OffsetDateTime openDate,
                                                    OffsetDateTime closeDate) {
        SessionDataDto task = new SessionDataDto();
        task.setOpenSessionDate(openDate);
        task.setCloseSessionDate(closeDate);
        task.setWorkDate(workDate);
        return task;
    }

    protected LeaveDayEntryDto generateLeaveDayEntryDto(LocalDate date, Double vacationAmount, Double sickAmount) {
        return LeaveDayEntryDto.builder().date(date).vacationAmount(vacationAmount).sickAmount(sickAmount).build();
    }

    protected LeaveReportRequestDto generateLeaveReportRequestDto(List<LeaveDayEntryDto> days) {
        return LeaveReportRequestDto.builder().days(days).build();
    }

    protected EditLeaveDayDto createEditLeaveDayDto(Integer id, LocalDate date, Double vacationAmount,
                                                    Double sickAmount) {
        return EditLeaveDayDto.builder()
                .id(id)
                .date(date)
                .vacationAmount(vacationAmount)
                .sickAmount(sickAmount)
                .build();
    }

    protected LeaveDay findLeaveDayOrFail(Integer id) {
        return leaveDaysRepository.findById(id).orElseThrow(
                () -> new AssertionError(String.format("Reason: expected a leave day with id:%s to exist", id)));
    }

    protected void verifyLeaveDayDbState(Integer id, LocalDate expectedDate, Double expectedVacation,
                                         Double expectedSick) {
        LeaveDay stored = findLeaveDayOrFail(id);
        assertEquals(expectedDate, stored.getLeaveDate(),
                "Reason: the stored date must match what the edit asked for");
        assertEquals(expectedVacation, stored.getVacationAmount(),
                "Reason: the stored vacation portion must match what the edit asked for");
        assertEquals(expectedSick, stored.getSickAmount(),
                "Reason: the stored sick portion must match what the edit asked for");
    }

    protected String leaveRange(String base) {
        return base + "?startDate=" + LEAVE_RANGE_START + "&endDate=" + LEAVE_RANGE_END;
    }
}
