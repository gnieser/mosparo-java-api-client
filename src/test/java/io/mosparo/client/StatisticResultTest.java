package io.mosparo.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StatisticResultTest {

    @Test
    void testStatisticResult() {
        Map<LocalDate, StatisticResult.Statistic> byDate = Map.of(
                LocalDate.of(2021, 4, 29),
                new StatisticResult.Statistic(2, 5));

        StatisticResult.StatisticData data = new StatisticResult.StatisticData(10, 20, byDate);
        StatisticResult sr = new StatisticResult(true, data, null, null);

        assertTrue(sr.getResult());
        assertEquals(10, sr.getData().getNumberOfValidSubmissions());
        assertEquals(20, sr.getData().getNumberOfSpamSubmissions());
        assertThat(sr.getData().getNumbersByDate()).isEqualTo(byDate);
    }

    @Test
    void testStatisticResultError() {
        StatisticResult sr = new StatisticResult(null, null, true, "Error");

        assertTrue(sr.hasError());
        assertEquals("Error", sr.getErrorMessage());
    }
}