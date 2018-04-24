package com.microsoft.identity.common.internal.logging;

import android.os.Build;
import android.util.Log;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class Logger {
    private static final Logger INSTANCE = new Logger();
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Turn on the VERBOSE level logging by default.
    private LogLevel mLogLevel = LogLevel.VERBOSE;
    private ILoggerCallback mExternalLogger;
    private final Object mLock = new Object();

    // Disable to log PII by default.
    private static boolean mAllowPii = false;
    // Disable to Logcat logging by default.
    private static boolean mAllowLogcat = false;

    /**
     * Enum class for LogLevel that the sdk recognizes.
     */
    public enum LogLevel {
        /**
         * Error level logging.
         */
        ERROR,
        /**
         * Warn level logging.
         */
        WARN,
        /**
         * Info level logging.
         */
        INFO,
        /**
         * Verbose level logging.
         */
        VERBOSE
    }

    /**
     * @return The single instance of {@link Logger}
     */
    public static Logger getInstance() {
        return INSTANCE;
    }

    /**
     * Enable/Disable log message with PII (personal identifiable information) info.
     * By default, the SDK doesn't log any PII.
     *
     * @param allowPii True if enabling PII info to be logged, false otherwise.
     */
    public static void setAllowPii(final boolean allowPii) {
        mAllowPii = allowPii;
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk disables it.
     *
     * @param allowLogcat True if enabling the logcat logging, false otherwise.
     */
    public static void setAllowLogcat(final boolean allowLogcat) {
        mAllowLogcat = allowLogcat;
    }

    /**
     * Get if log PII is enabled.
     */
    public static boolean getAllowPii() {
        return mAllowPii;
    }

    /**
     * Get if logcat is enabled.
     */
    public static boolean getAllowLogcat() {
        return mAllowLogcat;
    }

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level logging.
     *
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        mLogLevel = logLevel;
    }

    /**
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     *
     * @param externalLogger The reference to the {@link ILoggerCallback} that can
     *                       output the logs to the designated places.
     */
    public void setExternalLogger(final ILoggerCallback externalLogger) {
        synchronized (mLock) {
            mExternalLogger = externalLogger;
        }
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     */
    public static void error(final String tag, final String correlationID, final String errorMessage, final Throwable exception) {
        getInstance().log(tag, LogLevel.ERROR, correlationID, errorMessage, exception, false);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message with PII.
     */
    public static void errorPII(final String tag, final String correlationID, final String errorMessage, final Throwable exception) {
        getInstance().log(tag, LogLevel.ERROR, correlationID, errorMessage, exception, true);
    }

    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     */
    public static void warn(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.WARN, correlationID, message, null, false);
    }

    /**
     * Send a {@link LogLevel#WARN} log message with PII.
     */
    public static void warnPII(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.WARN, correlationID, message, null, true);
    }

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     */
    public static void info(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.INFO, correlationID, message, null, false);
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     */
    public static void infoPII(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.INFO, correlationID, message, null, true);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     */
    public static void verbose(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.VERBOSE, correlationID, message, null, false);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     */
    public static void verbosePII(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.VERBOSE, correlationID, message, null, true);
    }

    /**
     * TODO. Need to discuss on how to keep the correlationID. CorrelationID should be per request => need to sync with Telemetry implementation
     */
    private void log(final String tag, final LogLevel logLevel, final String correlationID,
                     final String message, final Throwable throwable, final boolean containsPII) {
        if (logLevel.compareTo(mLogLevel) > 0) {
            return;
        }

        // Developer turns off PII logging, if the log meLoggerSettingssage contains any PII, we should not send it.
        if (!mAllowPii && containsPII) {
            return;
        }

        //Format the log message.
        final String logMessage = formatMessage(correlationID, message, throwable);

        // Send logs into Logcat.
        if (mAllowLogcat) {
            sendLogcatLogs(tag, logLevel, logMessage.toString());
        }

        // Send logs into external logger callback.
        synchronized (mLock) {
            if (null != mExternalLogger) {
                mExternalLogger.log(tag, logLevel, logMessage, containsPII);
            }
        }

    }

    /**
     * Wrap the log message.
     * If correlation id exists:
     * <library_version> <platform> <platform_version> [<timestamp> - <correlation_id>] <log_message>
     * If correlation id doesn't exist:
     * <library_version> <platform> <platform_version> [<timestamp>] <log_message>
     */
    private String formatMessage(final String correlationID, final String message, final Throwable throwable) {
        final String logMessage = StringExtensions.isNullOrBlank(message) ? "N/A" : message;
        return " [" + getUTCDateTimeAsString()
                + (StringExtensions.isNullOrBlank(correlationID) ? "] " : " - " + correlationID + "] ")
                + logMessage
                + " Android " + Build.VERSION.SDK_INT
                + (throwable == null ? "" : '\n' + Log.getStackTraceString(throwable));
    }

    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(new Date());
    }

    /**
     * Send logs to logcat as the default logging if developer doesn't turn off the logcat logging.
     */
    private void sendLogcatLogs(final String tag, final LogLevel logLevel, final String message) {
        // Append additional message to the message part for logcat logging
        switch (logLevel) {
            case ERROR:
                Log.e(tag, message);
                break;
            case WARN:
                Log.w(tag, message);
                break;
            case INFO:
                Log.i(tag, message);
                break;
            case VERBOSE:
                Log.v(tag, message);
                break;
            default:
                throw new IllegalArgumentException("Unknown log level");
        }
    }
}