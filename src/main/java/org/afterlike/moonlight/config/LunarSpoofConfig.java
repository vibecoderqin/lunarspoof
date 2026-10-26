package org.afterlike.moonlight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** User-facing settings for LunarSpoof. Never stores Minecraft or Microsoft access tokens. */
public final class LunarSpoofConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile LunarSpoofConfig instance = new LunarSpoofConfig();

    public String brandMode = "lunar";
    public String customBrand = "custom-client";
    public boolean hideForgeHandshake = true;
    public boolean showPeerIcons = true;

    public boolean discordRpcEnabled = false;
    public String discordApplicationId = "";
    public String discordDetails = "Minecraft 1.8.9";
    public String discordState = "Lunar Client";
    public String discordLargeImageKey = "lunar_logo";
    public String discordLargeImageText = "Lunar Client • Minecraft 1.8.9";

    private LunarSpoofConfig() {
    }

    public static LunarSpoofConfig get() {
        return instance;
    }

    public static synchronized LunarSpoofConfig load() {
        File file = getFile();
        LunarSpoofConfig loaded = null;
        if (file.isFile()) {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, LunarSpoofConfig.class);
            } catch (Exception e) {
                LOGGER.warn("Could not read LunarSpoof config; using defaults", e);
            }
        }
        if (loaded == null) {
            // Preserve an unreadable file before defaults overwrite it. This lets users
            // recover manually-edited settings after an interrupted or invalid save.
            if (file.isFile()) {
                preserveCorruptConfig(file);
            }
            loaded = new LunarSpoofConfig();
        }
        loaded.sanitize();
        instance = loaded;
        loaded.save();
        return loaded;
    }

    public synchronized void save() {
        try {
            File file = getFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IllegalStateException("Unable to create config directory");
            }
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not save LunarSpoof config", e);
        }
    }

    private static void preserveCorruptConfig(File file) {
        File backup = new File(file.getParentFile(), file.getName() + ".corrupt");
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Preserved unreadable LunarSpoof config as {}", backup.getName());
        } catch (Exception e) {
            LOGGER.warn("Could not back up unreadable LunarSpoof config", e);
        }
    }

    public static File getFile() {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, "config"), "lunarspoof.json");
    }

    private void sanitize() {
        if (brandMode == null || !(brandMode.equalsIgnoreCase("lunar")
                || brandMode.equalsIgnoreCase("vanilla")
                || brandMode.equalsIgnoreCase("forge")
                || brandMode.equalsIgnoreCase("custom"))) {
            brandMode = "lunar";
        } else {
            brandMode = brandMode.toLowerCase(java.util.Locale.ROOT);
        }
        if (customBrand == null) customBrand = "custom-client";
        customBrand = sanitizeBrand(customBrand);
        if (discordApplicationId == null) discordApplicationId = "";
        discordApplicationId = discordApplicationId.trim();
        if (discordDetails == null || discordDetails.trim().isEmpty()) discordDetails = "Minecraft 1.8.9";
        discordDetails = trimTo(discordDetails, 128);
        if (discordState == null || discordState.trim().isEmpty()) discordState = "Lunar Client";
        discordState = trimTo(discordState, 128);
        if (discordLargeImageKey == null) discordLargeImageKey = "";
        discordLargeImageKey = trimTo(discordLargeImageKey, 256);
        if (discordLargeImageText == null) discordLargeImageText = "";
        discordLargeImageText = trimTo(discordLargeImageText, 128);
    }

    /** Strip control characters before a user-supplied brand reaches network payloads or logs. */
    public static String sanitizeBrand(String value) {
        if (value == null) return "custom-client";
        return trimTo(value.replaceAll("[\\p{Cntrl}]", ""), 128);
    }

    public static String trimTo(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public static boolean isValidApplicationId(String id) {
        return id != null && id.matches("[0-9]{15,22}");
    }
}
