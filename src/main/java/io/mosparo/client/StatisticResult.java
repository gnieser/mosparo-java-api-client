package io.mosparo.client;

import java.time.LocalDate;
import java.util.Map;

import lombok.Value;

/**
 * Represents the exact numbers of how many spam and valid submissions a mosparo project received in the last days.
 *
 * @see <a href="https://documentation.mosparo.io/docs/api/statistic#response">mosparo API documentation</a>
 */
@Value
public class StatisticResult {

    @Value
    public static class Statistic {
        int numberOfValidSubmissions;
        int numberOfSpamSubmissions;
    }

    @Value
    public static class StatisticData {
        int numberOfValidSubmissions;
        int numberOfSpamSubmissions;
        Map<LocalDate, Statistic> numbersByDate;
    }

    Boolean result;
    StatisticData data;
    Boolean error;
    String errorMessage;

    public boolean hasError() {
        return this.error != null && this.error;
    }
}