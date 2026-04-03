package com.medihelp.health.service;

import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.common.event.VitalsAnomalyEvent;
import com.medihelp.health.entity.VitalBaseline;
import com.medihelp.health.repository.VitalBaselineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final VitalBaselineRepository baselineRepository;
    private final RabbitTemplate rabbitTemplate;

    // Threshold bounds: [min, max]
    private static final Map<String, double[]> THRESHOLDS = Map.of(
            "HEART_RATE", new double[]{50, 120},
            "BLOOD_PRESSURE_SYSTOLIC", new double[]{90, 140},
            "BLOOD_PRESSURE_DIASTOLIC", new double[]{60, 90},
            "BLOOD_SUGAR", new double[]{70, 200},
            "TEMPERATURE", new double[]{96.0, 100.4},
            "OXYGEN_SATURATION", new double[]{92, 100}
    );

    @Transactional
    public boolean checkAnomaly(UUID userId, String vitalType, double value) {
        boolean isAnomaly = false;
        String anomalyType = null;
        String severity = "LOW";

        // Threshold check
        double[] bounds = THRESHOLDS.get(vitalType);
        if (bounds != null && (value < bounds[0] || value > bounds[1])) {
            isAnomaly = true;
            anomalyType = "THRESHOLD";
            double deviation = Math.max(bounds[0] - value, value - bounds[1]);
            double range = bounds[1] - bounds[0];
            severity = deviation > range * 0.5 ? "HIGH" : deviation > range * 0.25 ? "MEDIUM" : "LOW";
        }

        // Trend check (Welford's) — only if enough data
        VitalBaseline baseline = baselineRepository.findByUserIdAndVitalType(userId, vitalType).orElse(null);
        if (!isAnomaly && baseline != null && baseline.getCount() > 10) {
            double variance = baseline.getRunningVariance() / baseline.getCount();
            double stddev = Math.sqrt(variance);
            if (stddev > 0 && Math.abs(value - baseline.getRunningMean()) > 2 * stddev) {
                isAnomaly = true;
                anomalyType = "TREND";
                severity = Math.abs(value - baseline.getRunningMean()) > 3 * stddev ? "HIGH" : "MEDIUM";
            }
        }

        if (isAnomaly) {
            double threshold = bounds != null ? (value > bounds[1] ? bounds[1] : bounds[0]) : (baseline != null ? baseline.getRunningMean() : 0);
            publishAnomalyEvent(userId, vitalType, value, threshold, anomalyType, severity);
        }

        // Always update baseline
        updateBaseline(userId, vitalType, value);

        return isAnomaly;
    }

    private void updateBaseline(UUID userId, String vitalType, double value) {
        VitalBaseline baseline = baselineRepository.findByUserIdAndVitalType(userId, vitalType)
                .orElse(VitalBaseline.builder()
                        .userId(userId)
                        .vitalType(vitalType)
                        .runningMean(0.0)
                        .runningVariance(0.0)
                        .count(0L)
                        .build());

        baseline.setCount(baseline.getCount() + 1);
        double delta = value - baseline.getRunningMean();
        baseline.setRunningMean(baseline.getRunningMean() + delta / baseline.getCount());
        double delta2 = value - baseline.getRunningMean();
        baseline.setRunningVariance(baseline.getRunningVariance() + delta * delta2);

        baselineRepository.save(baseline);
    }

    private void publishAnomalyEvent(UUID userId, String vitalType, double value, double threshold, String anomalyType, String severity) {
        VitalsAnomalyEvent event = VitalsAnomalyEvent.builder()
                .userId(userId.toString())
                .vitalType(vitalType)
                .recordedValue(value)
                .threshold(threshold)
                .anomalyType(anomalyType)
                .severity(severity)
                .detectedAt(Instant.now())
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_VITALS_ANOMALY, event);
            log.warn("Vitals anomaly detected for user {}: {} = {} ({}:{} severity)", userId, vitalType, value, anomalyType, severity);
        } catch (Exception e) {
            log.error("Failed to publish anomaly event: {}", e.getMessage());
        }
    }
}
