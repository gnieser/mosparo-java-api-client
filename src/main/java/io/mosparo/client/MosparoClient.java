package io.mosparo.client;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * The {@code MosparoClient} interface provides methods to interact with the mosparo API,
 * including form verification, statistics retrieval, and health checks. Implementations of
 * this interface facilitate communication with Mosparo's backend services to ensure data
 * integrity and monitor system health.
 *
 * <p>
 * Key functionalities include:
 * <ul>
 * <li>Verifying form submissions to prevent data manipulation and bypassing protection mechanisms.</li>
 * <li>Retrieving statistics on spam and valid submissions over specified time ranges.</li>
 * <li>Performing health checks to monitor the status of Mosparo services.</li>
 * </ul>
 *
 * <p>
 * Implementations should handle communication errors and Mosparo-specific exceptions
 * gracefully, providing meaningful feedback to the caller.
 *
 * @see MosparoDefaultClient
 * @see <a href="https://documentation.mosparo.io/docs/api">Mosparo API Documentation</a>
 */
public interface MosparoClient {

    /**
     * Performs backend verification of a form protected by mosparo.
     * After successful verification, it is recommended that the user ensures all required fields are verified to confirm
     * that the form data has not been manipulated.
     * <p>
     * Use the {@link #verifySubmission(Map, Set)} method instead to handle the backend verification and check the required fields.
     *
     * @param formData The form data included the mosparo fields
     * @return the result of the verification
     * @throws IOException if a communication error occurs
     * @throws MosparoException if the verification fails, if the signatures do not match
     * @see <a href="https://documentation.mosparo.io/docs/api/verification#verify">Mosparo API Verification documentation</a>
     */
    VerificationResult verifySubmission(Map<String, Object> formData) throws IOException, MosparoException;

    /**
     * Performs backend verification of a form protected by mosparo and checks the required fields to ensure the protection
     * is not bypassed.
     * <p>
     * The required fields are those that must be verified to ensure the protection is not bypassed. These include not
     * only mandatory form inputs but all the fields where users can enter data. Essentially, amy field that accepts user input
     * should be considered a required field for verification.
     * 
     * @param formData The form data, included the mosparo fields
     * @param requiredFields The list of fields to check to ensure the protection is not bypassed
     * @return the result of the verification process
     * @throws IOException if a communication error occurs
     * @throws MosparoException if the verification fails, if the signatures do not match, or if the protection is being
     *         bypassed
     * @see <a href="https://documentation.mosparo.io/docs/api/verification#verify">API verify in mosparo documentation</a>
     */
    VerificationResult verifySubmission(Map<String, Object> formData, Set<String> requiredFields)
            throws IOException, MosparoException;

    /**
     * Returns the exact numbers of how many spam and valid submissions your project received in the specified time range.
     *
     * @param range The number of seconds for which mosparo should return the statistics. For example, {@code 3600} will return
     *        the numbers for the last hour. If {@code null}, all data from the last 14 days are used.
     * @param startDate Defines the starting date from which mosparo should return the statistics. This can be any date, but
     *        mosparo will
     *        only return available data (data may have already been deleted).
     * @return the statistic result containing the counts of spam and valid submissions
     * @throws IOException if a communication error occurs
     * @throws MosparoException if mosparo returns an error
     * @see <a href="https://documentation.mosparo.io/docs/api/statistic#by-date">API statistic by-date in mosparo
     *      documentation</a>
     */
    StatisticResult statisticByDate(Integer range, LocalDate startDate) throws IOException, MosparoException;

    /**
     * Checks the health status of mosparo and returns the relevant information
     *
     * @return a {@code HealthCheckResult} representing the health of mosparo
     * @throws IOException if a communication error occurs
     * @throws MosparoException if mosparo is unable to respond
     * @see <a href="https://documentation.mosparo.io/docs/api/health#check">API health check in mosparo documentation</a>
     */
    HealthCheckResult healthCheck() throws IOException, MosparoException;
}
