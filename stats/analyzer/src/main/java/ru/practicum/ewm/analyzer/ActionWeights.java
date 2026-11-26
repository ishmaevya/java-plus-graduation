package ru.practicum.ewm.analyzer;

import java.util.Map;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public final class ActionWeights {

    private static final Map<ActionTypeAvro, Double> WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    private ActionWeights() {
    }

    public static double get(ActionTypeAvro actionType) {
        return WEIGHTS.getOrDefault(actionType, 0.0);
    }
}
