package att.controller;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import att.dao.UserRepository;
import att.dao.UserTimeRepository;
import att.dto.EditDataTimeUserDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestExecutionListeners(
        listeners = { FlywayTestExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public abstract class BaseApiControllerTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserTimeRepository timeRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final Integer ADMIN_ID = 1;
    protected static final String ADMIN_PWD = "admin123";
    protected static final Integer USER_ID = 2;
    protected static final String USER_PWD = "user123";
    protected static final Integer OTHER_USER_ID = 3;
    protected static final String OTHER_USER_PWD = "other123";
    protected static final String ACCESS_DENIED = "Access Denied";

    protected static final String RECORD_URL = "/record";
    protected static final String START_URL = RECORD_URL + "/start/";
    protected static final String FINISH_URL = RECORD_URL + "/finish/";
    protected static final String RECORDS_URL = RECORD_URL + "/records";
    protected static final String RANGE_RECORDS_URL = RECORD_URL + "/range/records";
    protected static final String WORKDAYS_URL = RECORD_URL + "/workdays";
    protected static final String HOURS_URL = RECORD_URL + "/hours";
    protected static final String OVERTIME_URL = RECORD_URL + "/overtime";
    protected static final String CHECK_URL = RECORD_URL + "/check";
    protected static final String REMOVE_URL = RECORD_URL + "/remove/";

    protected static final String RANGE_START = "2024-01-01";
    protected static final String RANGE_END = "2024-01-31";

    protected static final String SEEDED_DAY = "2024-01-02";
    protected static final int SEEDED_TODAY_OPEN_ID = 1005;
    protected static final int SEEDED_FINISHED_ID = 1002;
    protected static final int SEEDED_OPEN_ID = 1003;

    private final RestTemplate rest = createRestTemplate();

    protected String jwtTokenAdministrator = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkiLCJleHAiOjIxMDEwMzI4MzEsImlhdCI6MTc4NTY3MjgzMSwiYXV0aG9yaXRpZXMiOlsiQURNSU4iLCJBRE1JTklTVFJBVE9SIiwiTU9ERVJBVE9SIiwiVVNFUiIsIkZBQ1RPUl9QQVNTV09SRCJdfQ.7v9mluTswU2F7GRdG5xKQxStTGn5bEy_5Dn74FYqShU";
    protected String jwtTokenUser = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJleHAiOjIxMDEwMzI3MDIsImlhdCI6MTc4NTY3MjcwMiwiYXV0aG9yaXRpZXMiOlsiVVNFUiIsIkZBQ1RPUl9QQVNTV09SRCJdfQ.x_Jobfo63CL4eUxU6lBO11SyMG7ZdQeO5Z3S5wyjbLY";

    @Bean
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

    /**
     * Helper to send HTTP requests with optional Basic Authentication
     */
    protected ResponseEntity<String> sendRequestWithAdmin(HttpMethod method, String path, Object requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenAdministrator);
        return sendRequest(method, path, requestBody, headers);
    }

    protected ResponseEntity<String> sendRequestWithUserRole(HttpMethod method, String path, Object requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtTokenUser);
        return sendRequest(method, path, requestBody, headers);
    }

    private ResponseEntity<String> sendRequest(HttpMethod method, String path, Object requestBody, HttpHeaders headers) {
        Object payload = (requestBody == null || requestBody instanceof String) ? requestBody : objectMapper.writeValueAsString(requestBody);
        return rest.exchange(url(path), method, new HttpEntity<>(payload, headers), String.class);
    }

    protected String url(String path){
        return "http://localhost:" + port + path;
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

    protected EditDataTimeUserDto createEditDataTimeUserDto(Integer id, LocalDateTime start, LocalDateTime finish) {
        return EditDataTimeUserDto.builder()
                .id(id)
                .start(start)
                .finish(finish)
                .build();
    }
}