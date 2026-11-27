package ru.practicum.ewm.analyzer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_interactions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_interactions_user_event", columnNames = {"user_id", "event_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "event_timestamp", nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Boolean viewed;
}
