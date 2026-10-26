package org.afterlike.moonlight.peers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.afterlike.moonlight.Moonlight;
import org.afterlike.moonlight.peers.auth.AuthClient;

public final class SubscriptionManager {
    private static volatile SubscriptionManager instance;
    private final Set<UUID> fetchedSet = ConcurrentHashMap.newKeySet();
    private volatile WSClient wsClient;

    private SubscriptionManager() {
    }

    public static void init() {
        instance = new SubscriptionManager();
        MinecraftForge.EVENT_BUS.register(instance);
    }

    public static boolean isConnected() {
        SubscriptionManager manager = instance;
        WSClient client = manager == null ? null : manager.wsClient;
        return client != null && client.isConnected();
    }

    /** Stop Lunar peer-service connections and discard cached peer data and auth state. */
    public static void disconnect() {
        SubscriptionManager manager = instance;
        if (manager != null) {
            WSClient oldClient = manager.wsClient;
            manager.wsClient = null;
            if (oldClient != null) {
                oldClient.disconnect();
            }
            manager.fetchedSet.clear();
        }
        PeerRegistry.getInstance().clear();
        Moonlight.get().clearAuth();
    }

    /**
     * Starts a fresh connection using the JWT already fetched for this session.
     * Subsequent reconnect attempts fetch a fresh JWT rather than reusing an expired one.
     */
    public static void connect(String jwt) {
        SubscriptionManager manager = instance;
        if (manager == null || !LunarSpoofConfigAccess.iconsEnabled()) {
            return;
        }

        WSClient oldClient = manager.wsClient;
        manager.wsClient = null;
        if (oldClient != null) {
            oldClient.disconnect();
        }
        manager.fetchedSet.clear();
        PeerRegistry.getInstance().clear();

        final WSClient client = new WSClient();
        final AtomicReference<String> initialJwt = new AtomicReference<>(jwt);
        manager.wsClient = client;
        client.setOnReconnect(() -> {
            if (instance == manager && manager.wsClient == client) {
                manager.fetchedSet.clear();
            }
        });
        client.connect(() -> {
            String cachedJwt = initialJwt.getAndSet(null);
            if (cachedJwt != null && !cachedJwt.trim().isEmpty()) {
                return cachedJwt;
            }
            return AuthClient.fetchJwt();
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        WSClient client = wsClient;
        if (event.phase != TickEvent.Phase.START || client == null || !client.isConnected()) {
            return;
        }
        syncWithTabList(client);
    }

    private void syncWithTabList(WSClient client) {
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler == null || mc.thePlayer == null) {
            return;
        }
        UUID self = mc.thePlayer.getUniqueID();
        Collection<NetworkPlayerInfo> tabList = netHandler.getPlayerInfoMap();
        Set<UUID> current = new java.util.HashSet<>();
        for (NetworkPlayerInfo info : tabList) {
            UUID uuid = info.getGameProfile().getId();
            if (!uuid.equals(self)) {
                current.add(uuid);
            }
        }
        List<UUID> toFetch = new ArrayList<>();
        for (UUID uuid : current) {
            if (!fetchedSet.contains(uuid)) {
                toFetch.add(uuid);
            }
        }
        if (!toFetch.isEmpty()) {
            fetchedSet.addAll(toFetch);
            client.loadTabLogos(toFetch);
        }
        List<UUID> left = new ArrayList<>();
        for (UUID uuid : fetchedSet) {
            if (!current.contains(uuid)) {
                left.add(uuid);
            }
        }
        if (!left.isEmpty()) {
            left.forEach(fetchedSet::remove);
            left.forEach(u -> PeerRegistry.getInstance().remove(u));
        }
    }

    /** Avoids starting/reconnecting the service after the privacy toggle was switched off. */
    private static final class LunarSpoofConfigAccess {
        private static boolean iconsEnabled() {
            return org.afterlike.moonlight.config.LunarSpoofConfig.get().showPeerIcons;
        }
    }
}
