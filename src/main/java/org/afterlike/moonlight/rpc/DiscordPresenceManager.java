package org.afterlike.moonlight.rpc;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.afterlike.moonlight.config.LunarSpoofConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional Discord Rich Presence. The status is published through the user's own
 * Discord application ID; no official Lunar Client application ID is impersonated.
 *
 * Lifecycle and IPC operations run off the Minecraft client thread.
 */
public final class DiscordPresenceManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long SESSION_START_SECONDS = System.currentTimeMillis() / 1000L;
    private static final ExecutorService RPC_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LunarSpoof-Discord-RPC");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile DiscordRpc client;
    private static volatile String activeApplicationId = "";

    private DiscordPresenceManager() {
    }

    public static void refresh() {
        RPC_EXECUTOR.execute(DiscordPresenceManager::refreshOnWorker);
    }

    private static void refreshOnWorker() {
        LunarSpoofConfig config = LunarSpoofConfig.get();
        String appId = config.discordApplicationId == null ? "" : config.discordApplicationId.trim();

        if (!config.discordRpcEnabled || !LunarSpoofConfig.isValidApplicationId(appId)) {
            stopLocked();
            return;
        }

        DiscordRpc active = client;
        if (active != null && appId.equals(activeApplicationId)) {
            publish(active, config);
            return;
        }

        stopLocked();
        final DiscordRpc newClient = new DiscordRpc();
        client = newClient;
        activeApplicationId = appId;

        try {
            newClient.init(appId, new RPCEventHandler() {
                @Override
                public void ready(User user) {
                    RPC_EXECUTOR.execute(() -> {
                        if (!isCurrent(newClient, appId)) {
                            return;
                        }
                        LOGGER.info("Discord Rich Presence connected");
                        publish(newClient, LunarSpoofConfig.get());
                    });
                }

                @Override
                public void disconnected(ErrorCode errorCode, String message) {
                    LOGGER.info("Discord Rich Presence disconnected: {} {}", errorCode, message);
                }

                @Override
                public void errored(ErrorCode errorCode, String message) {
                    LOGGER.warn("Discord Rich Presence error: {} {}", errorCode, message);
                }
            }, false);

            // A disable or Application ID change can arrive while init() is in progress.
            if (!isCurrent(newClient, appId)) {
                if (client == newClient) {
                    stopLocked();
                } else {
                    shutdownClient(newClient);
                }
                return;
            }
            publish(newClient, LunarSpoofConfig.get());
        } catch (Exception e) {
            LOGGER.warn("Could not initialize Discord Rich Presence. Ensure Discord desktop is running and the application ID is valid.", e);
            if (client == newClient) {
                client = null;
                activeApplicationId = "";
            }
            shutdownClient(newClient);
        }
    }

    public static void updatePresence() {
        RPC_EXECUTOR.execute(() -> {
            DiscordRpc active = client;
            if (active != null) {
                publish(active, LunarSpoofConfig.get());
            }
        });
    }

    /** Called by the JVM shutdown hook; wait briefly for the off-thread cleanup. */
    public static void shutdown() {
        try {
            Future<?> cleanup = RPC_EXECUTOR.submit(DiscordPresenceManager::stopLocked);
            cleanup.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.debug("Discord RPC cleanup did not finish before shutdown", e);
        }
    }

    private static boolean isCurrent(DiscordRpc rpc, String appId) {
        LunarSpoofConfig latest = LunarSpoofConfig.get();
        return client == rpc && appId.equals(activeApplicationId)
                && latest.discordRpcEnabled && appId.equals(latest.discordApplicationId);
    }

    /** Must only be called on the RPC worker thread. */
    private static void stopLocked() {
        DiscordRpc old = client;
        client = null;
        activeApplicationId = "";
        if (old != null) {
            shutdownClient(old);
        }
    }

    private static void shutdownClient(DiscordRpc rpc) {
        // Always attempt shutdown even if clearing the published activity fails.
        try {
            rpc.updatePresence(null);
        } catch (Exception e) {
            LOGGER.debug("Could not clear Discord Rich Presence before shutdown", e);
        }
        try {
            rpc.shutdown();
        } catch (Exception e) {
            LOGGER.debug("Error shutting down Discord RPC", e);
        }
    }

    private static void publish(DiscordRpc rpc, LunarSpoofConfig config) {
        if (rpc == null || !config.discordRpcEnabled) {
            return;
        }
        DiscordRichPresence.DiscordRichPresenceBuilder builder = DiscordRichPresence.builder()
                .name("Lunar Client")
                .details(LunarSpoofConfig.trimTo(config.discordDetails, 128))
                .state(LunarSpoofConfig.trimTo(config.discordState, 128))
                .startTimestamp(SESSION_START_SECONDS)
                .activityType(ActivityType.PLAYING);

        if (config.discordLargeImageKey != null && !config.discordLargeImageKey.trim().isEmpty()) {
            builder.largeImageKey(config.discordLargeImageKey.trim());
            if (config.discordLargeImageText != null && !config.discordLargeImageText.trim().isEmpty()) {
                builder.largeImageText(LunarSpoofConfig.trimTo(config.discordLargeImageText, 128));
            }
        }
        try {
            rpc.updatePresence(builder.build());
        } catch (Exception e) {
            LOGGER.debug("Could not update Discord Rich Presence", e);
        }
    }
}
