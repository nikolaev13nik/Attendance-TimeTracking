package att.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

import jakarta.annotation.PostConstruct;

@Configuration
public class WorkTimeConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WorkTimeConfiguration.class);
    private static final int MINUTES_PER_HOUR = 60;
    private static final long DEFAULT_OVERTIME_DAILY_THRESHOLD_MINUTES = 8L * MINUTES_PER_HOUR;

    @Value("${att.overtime.daily-threshold:8.0}")
    private String overtimeDailyThreshold;

    @PostConstruct
    void logOvertimeDailyThreshold() {
        Long minutes = parseMinutes(overtimeDailyThreshold);
        if (minutes == null) {
            log.warn("att.overtime.daily-threshold '{}' is not readable as H.MM, using default {}",
                    overtimeDailyThreshold, hoursAndMinutes(DEFAULT_OVERTIME_DAILY_THRESHOLD_MINUTES));
        } else {
            log.info("att.overtime.daily-threshold '{}' taken as {}", overtimeDailyThreshold,
                    hoursAndMinutes(minutes));
        }
    }

    public long getOvertimeDailyThresholdMinutes() {
        Long minutes = parseMinutes(overtimeDailyThreshold);
        return minutes == null ? DEFAULT_OVERTIME_DAILY_THRESHOLD_MINUTES : minutes;
    }

    private static String hoursAndMinutes(long minutes) {
        return String.format("%dh%02dm", minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR);
    }

    private static Long parseMinutes(String value) {
        try {
            BigDecimal threshold = new BigDecimal(value.trim());
            BigDecimal minutes = threshold.remainder(BigDecimal.ONE).movePointRight(2);
            if (threshold.signum() < 0 || minutes.compareTo(BigDecimal.valueOf(MINUTES_PER_HOUR)) >= 0
                    || minutes.stripTrailingZeros().scale() > 0) {
                return null;
            }
            return threshold.longValue() * MINUTES_PER_HOUR + minutes.longValue();
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
