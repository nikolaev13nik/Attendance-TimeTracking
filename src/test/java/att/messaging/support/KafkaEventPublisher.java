//package att.messaging.support;
//
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.DisposableBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//
//import java.util.Map;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
//import tools.jackson.databind.ObjectMapper;
//
/// **
// * Test-only helper for publishing straight onto a Kafka topic, bypassing the app's normal
// * {@code StreamBridge} producer path. Builds its own small {@link DefaultKafkaProducerFactory}/
// * {@link KafkaTemplate} rather than using Spring Boot's autoconfigured {@code KafkaTemplate} bean: that shared bean is
// * configured from the global {@code spring.kafka.producer.*} properties, which the Spring Cloud Stream
// * Kafka binder also reuses as its own defaults - overriding them here would silently change how the
// * production {@code StreamBridge} publishes real messages under test.
// * <p>
// * Deliberately not {@code @Component}: see {@link KafkaEventCapture} for why - it's registered only
// * where needed via {@code @Import} on {@link KafkaEventTestSupport}.
// */
//public class KafkaEventPublisher implements DisposableBean {
//
//    private final DefaultKafkaProducerFactory<String, String> producerFactory;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final ObjectMapper objectMapper;
//
//    public KafkaEventPublisher(@Value("${spring.embedded.kafka.brokers}") String bootstrapServers,
//                               ObjectMapper objectMapper) {
//        this.producerFactory = new DefaultKafkaProducerFactory<>(Map.of(
//                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
//                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
//                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));
//        this.kafkaTemplate = new KafkaTemplate<>(producerFactory);
//        this.objectMapper = objectMapper;
//    }
//
//    public void publishRaw(String topic, String key, String rawJson) {
//        try {
//            kafkaTemplate.send(topic, key, rawJson).get(5, TimeUnit.SECONDS);
//        } catch (ExecutionException | TimeoutException e) {
//            throw new RuntimeException("Failed to publish test message to topic " + topic, e);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Interrupted while publishing test message to topic " + topic, e);
//        }
//    }
//
//    public <T> void publish(String topic, String key, T payload) {
//        publishRaw(topic, key, objectMapper.writeValueAsString(payload));
//    }
//
//    @Override
//    public void destroy() {
//        producerFactory.destroy();
//    }
//}
