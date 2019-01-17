package io.auklet.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.auklet.Auklet;
import io.auklet.AukletException;
import io.auklet.core.DataUsageConfig;
import io.auklet.core.Util;
import mjson.Json;
import net.jcip.annotations.NotThreadSafe;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>The <i>data usage limit file</i> contains the configuration values, for this agent's app ID,
 * that control how much data the agent emits to the sink.</p>
 */
@NotThreadSafe
public final class DataUsageLimit extends AbstractJsonConfigFileFromApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataUsageLimit.class);
    private static final Long MEGABYTES_TO_BYTES = 1000000L;
    private static final Long SECONDS_TO_MILLISECONDS = 1000L;

    private DataUsageConfig usageConfig;

    @Override public void start(@NonNull Auklet agent) throws AukletException {
        LOGGER.debug("Loading data usage limits file.");
        super.start(agent);
        Json config = this.loadConfig();
        this.updateConfig(config);
    }

    @Override public String getName() { return "limits"; }

    /**
     * <p>Returns the underlying data usage limit config object.</p>
     *
     * @return never {@code null}.
     */
    @NonNull public DataUsageConfig getConfig() { return this.usageConfig; }

    /** <p>Refreshes the data usage limit config from the API.</p> */
    public void refresh() {
        try {
            Json config = this.fetchFromApi();
            this.writeToDisk(config);
            this.updateConfig(config);
        } catch (AukletException e) {
            LOGGER.warn("Could not refresh data usage limit config from API.", e);
        }
    }

    @Override protected Json readFromDisk() {
        try {
            return Util.validateJson(Util.readJson(this.getStringFromDisk()), this.getClass().getName()).at("config");
        } catch (AukletException | IOException | IllegalArgumentException e) {
            LOGGER.warn("Could not read data usage limits file from disk, will re-download from API.", e);
            return null;
        }
    }

    @Override protected Json fetchFromApi() throws AukletException {
        String apiSuffix = String.format("/private/devices/%s/app_config/", this.getAgent().getAppId());
        Request.Builder request = new Request.Builder()
                .url(this.getAgent().getBaseUrl() + apiSuffix).get()
                .header("Content-Type", "application/json; charset=utf-8");
        return this.makeJsonRequest(request).at("config");
    }

    @Override protected void writeToDisk(@NonNull Json contents) throws AukletException {
        if (contents == null) throw new AukletException("Input is null.");
        Util.writeUtf8(this.file, contents.toString());
    }

    /**
     * <p>Updates this object with the config values from the JSON.</p>
     *
     * @param config never {@code null}.
     * @throws AukletException if the input is {@code null}.
     */
    private void updateConfig(@NonNull Json config) throws AukletException {
        if (config == null) throw new AukletException("Data usage limit JSON is null.");
        long emissionPeriod = config.at("emission_period").asLong() * SECONDS_TO_MILLISECONDS;
        long storageLimit = config.at("storage").at("storage_limit", 0L).asLong() * MEGABYTES_TO_BYTES;
        long cellularDataLimit = config.at("data").at("cellular_data_limit", 0L).asLong() * MEGABYTES_TO_BYTES;
        int cellularPlanDate = config.at("data").at("normalized_cell_plan_date").asInteger();
        this.usageConfig = new DataUsageConfig(emissionPeriod, storageLimit, cellularDataLimit, cellularPlanDate);
    }

}
