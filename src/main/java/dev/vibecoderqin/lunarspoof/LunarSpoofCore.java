package dev.vibecoderqin.lunarspoof;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

/** Runtime helper called by the bytecode transformer after Join Game handling. */
public final class LunarSpoofCore {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static NetworkManager lastRegisteredManager;

    private LunarSpoofCore() {
    }

    public static void afterJoinGame() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }

        EntityPlayerSP player = minecraft.thePlayer;
        NetHandlerPlayClient handler = minecraft.getNetHandler();
        if (player == null || handler == null) {
            return;
        }

        NetworkManager manager = handler.getNetworkManager();
        if (manager == null || manager == lastRegisteredManager) {
            return;
        }

        manager.sendPacket(new C17PacketCustomPayload(
                "REGISTER",
                new PacketBuffer(Unpooled.wrappedBuffer("Lunar-Client".getBytes(UTF_8)))
        ));

        UUID uuid = player.getUniqueID();
        byte[] authenticationPayload = ByteBuffer.allocate(17)
                .put((byte) 26)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();

        manager.sendPacket(new C17PacketCustomPayload(
                "Lunar-Client",
                new PacketBuffer(Unpooled.wrappedBuffer(authenticationPayload))
        ));
        lastRegisteredManager = manager;
    }
}
