package ru.practicum.ewm.aggregator;

import java.util.concurrent.ExecutionException;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionListener {

    private final SimilarityProcessor similarityProcessor;
    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
    private final String similarityTopic;

    public UserActionListener(SimilarityProcessor similarityProcessor,
                              KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                              @Value("${aggregator.topic.event-similarity}") String similarityTopic) {
        this.similarityProcessor = similarityProcessor;
        this.kafkaTemplate = kafkaTemplate;
        this.similarityTopic = similarityTopic;
    }

    @KafkaListener(topics = "${aggregator.topic.user-actions}",
            groupId = "${aggregator.consumer.group-id}")
    public void onMessage(ConsumerRecord<Long, UserActionAvro> record, Acknowledgment acknowledgment)
            throws InterruptedException, ExecutionException {
        for (EventSimilarityAvro similarity : similarityProcessor.process(record.value())) {
            kafkaTemplate.send(similarityTopic, similarityKey(similarity), similarity).get();
        }
        acknowledgment.acknowledge();
    }

    private static String similarityKey(EventSimilarityAvro similarity) {
        return similarity.getEventA() + "_" + similarity.getEventB();
    }
}
