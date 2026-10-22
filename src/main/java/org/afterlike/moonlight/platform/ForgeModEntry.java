package org.afterlike.moonlight.platform;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.afterlike.moonlight.Moonlight;
import org.afterlike.moonlight.apollo.ApolloHandler;
import org.afterlike.moonlight.command.LunarSpoofCommand;
import org.afterlike.moonlight.config.LunarSpoofConfig;
import org.afterlike.moonlight.peers.SubscriptionManager;
import org.afterlike.moonlight.peers.auth.AuthClient;
import org.afterlike.moonlight.rpc.DiscordPresenceManager;
import org.afterlike.moonlight.version.MoonlightConfig;
import org.afterlike.moonlight.version.VersionFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "lunarspoof", name = "LunarSpoof", version = "1.1.0", clientSideOnly = true, useMetadata = false)
public class ForgeModEntry {
    private static final Logger LOGGER = LogManager.getLogger();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LunarSpoofConfig spoofConfig = LunarSpoofConfig.load();
        Moonlight.get().configureBrand(spoofConfig.brandMode, spoofConfig.customBrand);

        LOGGER.info("LunarSpoof {} initializing...", Moonlight.get().getVersion());
        MoonlightConfig config = MoonlightConfig.load();
        Moonlight.get().setModStatusSeed(config.modStatusSeed);
        if (config.lunarVersion != null && config.lunarGitCommit != null) {
            Moonlight.get().setLunarVersion(config.lunarVersion, config.lunarGitCommit);
        }
        if (config.isStale()) {
            VersionFetcher fetcher = new VersionFetcher(config);
            if (Moonlight.get().getLunarGitCommit() == null) {
                fetcher.fetch();
            } else {
                fetcher.fetchAsync();
            }
        }

        ApolloHandler.init();
        SubscriptionManager.init();
        ClientCommandHandler.instance.registerCommand(new LunarSpoofCommand());
        DiscordPresenceManager.refresh();
        Runtime.getRuntime().addShutdownHook(new Thread(DiscordPresenceManager::shutdown, "LunarSpoof-RPC-Shutdown"));
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Peer-icon detection uses the Lunar authentication and websocket endpoints.
     * The raw Minecraft session token is not saved by LunarSpoof.
     */
    @SubscribeEvent
    public void onConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!LunarSpoofConfig.get().showPeerIcons) {
            SubscriptionManager.disconnect();
            return;
        }
        UUID currentUuid = Minecraft.getMinecraft().getSession().getProfile().getId();
        if (currentUuid != null && currentUuid.equals(Moonlight.get().getAuthenticatedUuid())
                && SubscriptionManager.isConnected()) {
            return;
        }
        Moonlight.get().clearAuth();
        AuthClient.fetchJwtAsync(jwt -> {
            if (!LunarSpoofConfig.get().showPeerIcons) {
                return;
            }
            Moonlight.get().setAuthenticatorJwt(jwt, currentUuid);
            SubscriptionManager.connect(jwt);
        }, err -> LOGGER.warn("Lunar peer-icon connection unavailable; brand spoofing still works", err));
    }

    @Mod.EventHandler
    public void onPostInit(net.minecraftforge.fml.common.event.FMLPostInitializationEvent event) {
        DiscordPresenceManager.refresh();
    }
}
