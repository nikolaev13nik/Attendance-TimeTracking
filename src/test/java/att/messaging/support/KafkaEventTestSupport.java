//package att.messaging.support;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Import;
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.ContainerTestUtils;
//import org.springframework.test.context.TestPropertySource;
//
//import java.time.LocalDate;
//import java.time.OffsetDateTime;
//
//import att.controller.BaseApiControllerTest;
//import att.model.MonthStatistic;
//import att.model.MonthStatisticKey;
//
//import static att.controller.BaseApiControllerTest.MONTH_STATISTIC_DLQ_TOPIC;
//import static att.controller.BaseApiControllerTest.MONTH_STATISTIC_TOPIC;
//import static att.controller.BaseApiControllerTest.USER_STATISTIC_AI_ANALYSIS_TOPIC;
//
/// **
// * Shared base for Kafka test classes: one embedded broker, one {@link KafkaEventCapture} listener and
// * one {@link KafkaEventPublisher}, all Spring-lifecycle-managed. Subclasses need no Kafka setup/teardown
// * of their own beyond using {@code kafkaCapture}/{@code kafkaPublisher}.
// * <p>
// * Spring's test-context cache treats every subclass as the same context (identical
// * {@code @EmbeddedKafka}/{@code @TestPropertySource}), so the embedded broker and the capture listener's
// * container are shared across all Kafka test classes, not just within one class - {@link #clearKafkaCapture()}
// * matters across class boundaries too.
// * <p>
// * Deliberately does NOT set {@code spring.kafka.consumer.*}/{@code producer.*} serializer properties here:
// * the Spring Cloud Stream Kafka binder reuses those same global properties as its own defaults, so a
// * blanket override here would also change how the production {@code StreamBridge}/binder (de)serializes
// * real messages under test. {@link KafkaEventCapture} and {@link KafkaEventPublisher} scope their own
// * (de)serializers locally instead.
// * <p>
// * {@link KafkaEventCapture} and {@link KafkaEventPublisher} are registered via {@code @Import} rather
// * than {@code @Component} - see their javadoc for why: a plain {@code @Component} in this package would
// * be pulled into every {@code @SpringBootTest} context, including ones with no embedded broker.
// */
//@EmbeddedKafka(partitions = 1, topics = {
//        MONTH_STATISTIC_TOPIC,
//        USER_STATISTIC_AI_ANALYSIS_TOPIC,
//        MONTH_STATISTIC_DLQ_TOPIC
//})
//@TestPropertySource(properties = {
//        "spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
//        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
//})
//@Import({KafkaEventCapture.class, KafkaEventPublisher.class})
//public abstract class KafkaEventTestSupport extends BaseApiControllerTest {
//
//    private static final int CAPTURED_TOPIC_PARTITIONS = 3;
//
//    @Autowired
//    protected KafkaEventCapture kafkaCapture;
//
//    @Autowired
//    protected KafkaEventPublisher kafkaPublisher;
//
//    @Autowired
//    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
//
//    @BeforeEach
//    void clearKafkaCapture() {
//        // On the very first test in the (context-cached, possibly-shared) Spring context, the capture
//        // listener's consumer group has just been created and may not have finished its initial
//        // partition assignment yet - waiting here (a no-op once already assigned) avoids racing a
//        // publish against that one-time startup cost instead of relying on a bigger per-test timeout.
//        ContainerTestUtils.waitForAssignment(
//                kafkaListenerEndpointRegistry.getListenerContainer(KafkaEventCapture.LISTENER_ID),
//                CAPTURED_TOPIC_PARTITIONS);
//        kafkaCapture.clear();
//    }
//
//    protected MonthStatistic historicalStat(int tenantId, int userId, String monthStart, int workDays,
//                                            double overtimeHours, double totalWorkHours,
//                                            double vacationDays, double sickDays) {
//        MonthStatisticKey key = new MonthStatisticKey(tenantId, userId, LocalDate.parse(monthStart));
//        return new MonthStatistic(key, workDays, overtimeHours, totalWorkHours, vacationDays, sickDays,
//                "test", OffsetDateTime.now());
//    }
//}
