package ru.practicum.ewm.analyzer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
public class EventSimilarityListener {

    private final AnalyzerState analyzerState;

    public EventSimilarityListener(AnalyzerState analyzerState) {
        this.analyzerState = analyzerState;
    }

    @KafkaListener(topics = "${analyzer.topic.event-similarity}",
            groupId = "${analyzer.consumer.similarity-group-id}",
            properties = {
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=ru.practicum.kafka.deserializer.EventSimilarityDeserializer"
            })
    public void onMessage(ConsumerRecord<String, EventSimilarityAvro> record,
                          Acknowledgment acknowledgment) {
        analyzerState.registerSimilarity(record.value());
        acknowledgment.acknowledge();
    }
}
