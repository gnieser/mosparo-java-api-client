package io.mosparo.client;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public interface MosparoClient {

    /**
     * Performs the backend verification of a form protected by mosparo.
     * After successful verification, one should ensure all your required fields are verified
     * 
     * @see #verifySubmission(Map, Set) for a method that check the required fields.
     *
     * @param formData The form data included the mosparo fields
     * @return the result of the verification
     * @throws IOException if a communication error occurs
     * @throws MosparoException if the verification fails, if the signatures mismatch
     * @see <a href="https://documentation.mosparo.io/docs/api/verification#verify">API verify in mosparo documentation</a>
     */
    VerificationResult verifySubmission(Map<String, Object> formData) throws IOException, MosparoException;

    /**
     * Performs the backend verification of a form protected by mosparo and checks the required fields to ensure the protection
     * is not by-passed
     *
     * @param formData The form data included the mosparo fields
     * @param requiredFields The list of fields to check to ensure the protection is not by-passed
     * @return the result of the verification
     * @throws IOException if a communication error occurs
     * @throws MosparoException if the verification fails, if the signatures mismatch, if the protection is being by-passed
     * @see <a href="https://documentation.mosparo.io/docs/api/verification#verify">API verify in mosparo documentation</a>
     */
    VerificationResult verifySubmission(Map<String, Object> formData, Set<String> requiredFields)
            throws IOException, MosparoException;

    /**
     * Returns the exact numbers of how many spam and valid submissions your project received in the last days.
     *
     * @param range The number of seconds for which mosparo should return the numbers (3600 will return the numbers for the last
     *        hour). If {@code null}, all data from the last 14 days are used.
     * @param startDate Defines the date from which mosparo should return the statistics. This can be any date, but mosparo will
     *        return only the available data (the data could have already been deleted again).
     * @return the statistic result
     * @throws IOException if a communication error occurs
     * @throws MosparoException if the mosparo returns an error
     * @see <a href="https://documentation.mosparo.io/docs/api/statistic#by-date">API statistic by-date in mosparo
     *      documentation</a>
     */
    StatisticResult statisticByDate(Integer range, LocalDate startDate) throws IOException, MosparoException;

    /**
     * Checks mosparo health and returns the information
     *
     * @return a HealthCheckResult representing the health of mosparo
     * @throws IOException if a communication error occurs
     * @throws MosparoException if mosparo is unable to respond
     * @see <a href="https://documentation.mosparo.io/docs/api/health#check">API health check in mosparo documentation</a>
     */
    HealthCheckResult healthCheck() throws IOException, MosparoException;
}
