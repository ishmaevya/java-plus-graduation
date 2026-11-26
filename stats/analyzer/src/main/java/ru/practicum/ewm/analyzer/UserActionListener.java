package ru.practicum.ewm.analyzer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionListener {

    private final AnalyzerState analyzerState;

    public UserActionListener(AnalyzerState analyzerState) {
        this.analyzerState = analyzerState;
    }

    @KafkaListener(topics = "${analyzer.topic.user-actions}",
            groupId = "${analyzer.consumer.actions-group-id}")
    public void onMessage(ConsumerRecord<Long, UserActionAvro> record, Acknowledgment acknowledgment) {
        analyzerState.registerAction(record.value());
        acknowledgment.acknowledge();
    }
}
