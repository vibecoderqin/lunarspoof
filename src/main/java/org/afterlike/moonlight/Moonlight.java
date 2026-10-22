package org.afterlike.moonlight;

import java.util.Objects;
import java.util.UUID;
import org.afterlike.moonlight.config.LunarSpoofConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runtime state for LunarSpoof. The brand is only an unauthenticated client-reported
 * identifier; it is not proof of running the official Lunar Client.
 */
public class Moonlight {
    private static final Logger logger = LogManager.getLogger(Moonlight.class);
    private static final Moonlight instance = new Moonlight();
    private static final String FALLBACK_LUNAR_VERSION = "v2.23.5-2642";
    private static final String VERSION = org.afterlike.moonlight.BuildConstants.VERSION;

    private volatile String lunarVersion = emptyToNull(BuildConstants.LUNAR_VERSION);
    private volatile String lunarGitCommit = emptyToNull(BuildConstants.LUNAR_GIT_COMMIT);
    private volatile String brandMode = "lunar";
    private volatile String customBrand = "custom-client";
    private volatile String authenticatorJwt;
    private volatile UUID authenticatedUuid;
    private volatile long modStatusSeed;

    private Moonlight() {
    }

    public static Moonlight get() {
        return Objects.requireNonNull(instance);
    }

    public String getVersion() {
        return VERSION;
    }

    public String getLunarVersion() {
        return lunarVersion == null ? FALLBACK_LUNAR_VERSION : lunarVersion;
    }

    public String getLunarGitCommit() {
        return lunarGitCommit;
    }

    public String getClientBrand() {
        String mode = brandMode;
        if ("vanilla".equals(mode)) {
            return "vanilla";
        }
        if ("forge".equals(mode)) {
            return "fml,forge";
        }
        if ("custom".equals(mode)) {
            return customBrand == null || customBrand.trim().isEmpty() ? "custom-client" : customBrand.trim();
        }
        String version = getLunarVersion();
        if (version != null && !version.isEmpty() && !version.startsWith("v")) {
            version = "v" + version;
        }
        return "lunarclient:" + version;
    }

    public void configureBrand(String mode, String customValue) {
        if (mode == null) mode = "lunar";
        mode = mode.toLowerCase(java.util.Locale.ROOT);
        if (!"lunar".equals(mode) && !"vanilla".equals(mode) && !"forge".equals(mode) && !"custom".equals(mode)) {
            mode = "lunar";
        }
        this.brandMode = mode;
        this.customBrand = customValue == null ? "custom-client" : customValue.trim();
        logger.info("Client brand mode configured as {} ({})", mode, getClientBrand());
    }

    public boolean shouldHideForgeHandshake() {
        return LunarSpoofConfig.get().hideForgeHandshake;
    }

    public void setLunarVersion(String version, String gitCommit) {
        if (version != null && !version.trim().isEmpty()) {
            this.lunarVersion = version.trim();
        }
        if (gitCommit != null && !gitCommit.trim().isEmpty()) {
            this.lunarGitCommit = gitCommit.trim();
        }
        logger.info("Updated Lunar client-brand target: {} ({})", getClientBrand(),
                gitCommit == null ? "unknown" : gitCommit.substring(0, Math.min(7, gitCommit.length())));
    }

    public String getAuthenticatorJwt() {
        return authenticatorJwt;
    }

    public UUID getAuthenticatedUuid() {
        return authenticatedUuid;
    }

    public void setAuthenticatorJwt(String jwt, UUID uuid) {
        this.authenticatorJwt = jwt;
        this.authenticatedUuid = uuid;
    }

    public long getModStatusSeed() {
        return modStatusSeed;
    }

    public void setModStatusSeed(long seed) {
        this.modStatusSeed = seed;
    }

    public void clearAuth() {
        this.authenticatorJwt = null;
        this.authenticatedUuid = null;
    }

    private static String emptyToNull(String value) {
        return value != null && !value.isEmpty() ? value : null;
    }
}
