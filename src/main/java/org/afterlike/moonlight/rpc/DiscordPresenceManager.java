package org.afterlike.moonlight.rpc;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import org.afterlike.moonlight.config.LunarSpoofConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional Discord Rich Presence. The status is published through the user's own
 * Discord application ID; no official Lunar Client application ID is impersonated.
 */
public final class DiscordPresenceManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long SESSION_START_SECONDS = System.currentTimeMillis() / 1000L;
    private static volatile DiscordRpc client;
    private static volatile String activeApplicationId = "";

    private DiscordPresenceManager() {
    }

    public static synchronized void refresh() {
        LunarSpoofConfig config = LunarSpoofConfig.get();
        String appId = config.discordApplicationId == null ? "" : config.discordApplicationId.trim();

        if (!config.discordRpcEnabled || !LunarSpoofConfig.isValidApplicationId(appId)) {
            stopLocked();
            return;
        }

        if (client != null && appId.equals(activeApplicationId)) {
            publish(client, config);
            return;
        }

        stopLocked();
        final DiscordRpc newClient = new DiscordRpc();
        client = newClient;
        activeApplicationId = appId;

        Thread starter = new Thread(() -> {
            try {
                newClient.init(appId, new RPCEventHandler() {
                    @Override
                    public void ready(User user) {
                        LOGGER.info("Discord Rich Presence connected");
                        LunarSpoofConfig latest = LunarSpoofConfig.get();
                        if (latest.discordRpcEnabled && appId.equals(latest.discordApplicationId)) {
                            publish(newClient, latest);
                        }
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

                LunarSpoofConfig latest = LunarSpoofConfig.get();
                if (latest.discordRpcEnabled && appId.equals(latest.discordApplicationId)) {
                    publish(newClient, latest);
                }
            } catch (Exception e) {
                LOGGER.warn("Could not initialize Discord Rich Presence. Ensure Discord desktop is running and the application ID is valid.", e);
                synchronized (DiscordPresenceManager.class) {
                    if (client == newClient) {
                        client = null;
                        activeApplicationId = "";
                    }
                }
                try {
                    newClient.shutdown();
                } catch (Exception ignored) {
                }
            }
        }, "LunarSpoof-Discord-RPC");
        starter.setDaemon(true);
        starter.start();
    }

    public static synchronized void updatePresence() {
        if (client != null) {
            publish(client, LunarSpoofConfig.get());
        }
    }

    public static synchronized void shutdown() {
        stopLocked();
    }

    private static void stopLocked() {
        DiscordRpc old = client;
        client = null;
        activeApplicationId = "";
        if (old != null) {
            try {
                old.updatePresence(null);
                old.shutdown();
            } catch (Exception e) {
                LOGGER.debug("Error shutting down Discord RPC", e);
            }
        }
    }

    private static void publish(DiscordRpc rpc, LunarSpoofConfig config) {
        if (rpc == null || !config.discordRpcEnabled) {
            return;
        }
        DiscordRichPresence.Builder builder = DiscordRichPresence.builder()
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
