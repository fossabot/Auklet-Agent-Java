package io.auklet.platform.metrics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.auklet.AukletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * <p>This class handles retrieving memory and CPU usage for Android devices.</p>
 *
 * <p>CPU usage can only be retrieved on Android 7 or lower. When running on Android 8+,
 * this class will always report {@code 0} for CPU usage.</p>
 */
public final class AndroidMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidMetrics.class);
    private final ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();

    private long total = 0L;
    private long totalBefore = 0L;
    private long totalDiff = 0L;
    private long work = 0L;
    private long workBefore = 0L;
    private long workDiff = 0L;
    private float cpuUsage = 0;

    /**
     * <p>Creates the {@link ActivityManager} which is used to collect memory usage.</p>
     *
     * @param context the Android context.
     * @throws AukletException if context is {@code null}.
     */
    public AndroidMetrics(@NonNull Context context) throws AukletException {
        if (context == null) throw new AukletException("Android context is null.");
        if (Build.VERSION.SDK_INT >= 26) LOGGER.warn("Running on Android 8 or higher; system CPU stats will not be available.");
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        manager.getMemoryInfo(memInfo);
    }

    @Nullable public Runnable calculateCpuUsage() {
        if (Build.VERSION.SDK_INT >= 26) return null;
        return new Runnable() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
                    // Obtain current CPU Load
                    String[] s = reader.readLine().split("[ ]+", 9);
                    work = Long.parseLong(s[1]) + Long.parseLong(s[2]) + Long.parseLong(s[3]);
                    total = work + Long.parseLong(s[4]) + Long.parseLong(s[5]) +
                            Long.parseLong(s[6]) + Long.parseLong(s[7]);
                    // Calculate CPU Percentage
                    if (totalBefore != 0) {
                        workDiff = work - workBefore;
                        totalDiff = total - totalBefore;
                        cpuUsage = workDiff * 100 / (float) totalDiff;
                    }
                    totalBefore = total;
                    workBefore = work;
                } catch (IOException e) {
                    LOGGER.warn("Unable to obtain CPU usage", e);
                    return;
                }
            }
        };
    }

    /**
     * <p>Returns the memory usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value.
     */
    public double getMemoryUsage() {
        // memInfo.totalMem needs API 16+
        return memInfo.availMem / (double) memInfo.totalMem * 100.0;
    }

    /**
     * <p>Returns the CPU usage of the OS on which this agent is running.</p>
     *
     * @return a non-negative value.
     */
    public float getCpuUsage() {
        return cpuUsage;
    }

}
