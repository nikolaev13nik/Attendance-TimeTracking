//package att.messaging.support;
//
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.springframework.kafka.annotation.KafkaListener;
//
//import java.time.Duration;
//import java.util.Map;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Predicate;
//
/// **
// * Test-only capture of every message published on an {@code att.*} Kafka topic, replacing a
// * hand-rolled {@code KafkaConsumer} per test class. The listener container is created once when the
// * Spring test context starts and stays subscribed for its whole lifetime, so there is no per-test-method
// * consumer group and therefore no seek-to-end race to guard against - {@link #clear()} between tests is
// * enough for isolation.
// * <p>
// * Deliberately not {@code @Component}: this class lives under {@code att.messaging.support}, a
// * subpackage of the app's component-scan root, so a plain {@code @Component} here would be pulled into
// * every {@code @SpringBootTest} context - including plain controller tests with no embedded Kafka broker
// * and no {@code spring.embedded.kafka.brokers} property to satisfy its listener. It's registered only
// * where needed via {@code @Import} on {@link KafkaEventTestSupport}.
// */
//public class KafkaEventCapture {
//
//    /** Listener id, used by {@link KafkaEventTestSupport} to wait for its initial partition assignment. */
//    public static final String LISTENER_ID = "kafkaEventCaptureListener";
//
//    private final Map<String, BlockingQueue<ConsumerRecord<String, String>>> queuesByTopic = new ConcurrentHashMap<>();
//
//    // String (de)serializers are scoped to just this listener via `properties`, not the global
//    // spring.kafka.consumer.* properties - the Spring Cloud Stream Kafka binder reuses those same
//    // global properties as its own producer/consumer defaults, so setting them app-wide would also
//    // change how the production StreamBridge/binder (de)serializes real messages under test.
//    @KafkaListener(id = LISTENER_ID, topicPattern = "att\\..*", groupId = "kafka-event-capture-test", properties = {
//            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG + "=org.apache.kafka.common.serialization.StringDeserializer",
//            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG + "=org.apache.kafka.common.serialization.StringDeserializer",
//            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG + "=latest"
//    })
//    void capture(ConsumerRecord<String, String> record) {
//        queuesByTopic.computeIfAbsent(record.topic(), topic -> new LinkedBlockingQueue<>()).add(record);
//    }
//
//    public ConsumerRecord<String, String> awaitRecord(String topic, Duration timeout,
//                                                       Predicate<ConsumerRecord<String, String>> predicate) {
//        BlockingQueue<ConsumerRecord<String, String>> queue = queueFor(topic);
//        long deadlineNanos = System.nanoTime() + timeout.toNanos();
//        while (true) {
//            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
//            if (remainingMillis <= 0) {
//                return null;
//            }
//            ConsumerRecord<String, String> record;
//            try {
//                record = queue.poll(remainingMillis, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                throw new RuntimeException("Interrupted while awaiting a Kafka record on topic " + topic, e);
//            }
//            if (record == null) {
//                return null;
//            }
//            if (predicate.test(record)) {
//                return record;
//            }
//        }
//    }
//
//    public boolean awaitNone(String topic, Duration timeout, Predicate<ConsumerRecord<String, String>> predicate) {
//        return awaitRecord(topic, timeout, predicate) == null;
//    }
//
//    public void clear() {
//        queuesByTopic.values().forEach(BlockingQueue::clear);
//    }
//
//    private BlockingQueue<ConsumerRecord<String, String>> queueFor(String topic) {
//        return queuesByTopic.computeIfAbsent(topic, t -> new LinkedBlockingQueue<>());
//    }
//}
