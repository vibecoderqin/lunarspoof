package org.afterlike.moonlight.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import org.afterlike.moonlight.Moonlight;
import org.afterlike.moonlight.config.LunarSpoofConfig;
import org.afterlike.moonlight.gui.LunarSpoofClickGui;
import org.afterlike.moonlight.rpc.DiscordPresenceManager;

/** Client-only settings command. Arguments are never sent to the server. */
public final class LunarSpoofCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "lunarspoof";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/lunarspoof";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("lspoof", "ls");
    }

    @Override
    public int getRequiredPermissionLevel() {
        // This is a client-only settings GUI; it must work without server OP permissions.
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        LunarSpoofConfig config = LunarSpoofConfig.get();
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            Minecraft.getMinecraft().displayGuiScreen(new LunarSpoofClickGui());
            return;
        }
        if ("help".equalsIgnoreCase(args[0])) {
            help(sender);
            return;
        }

        String group = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("status".equals(group)) {
            status(sender, config);
            return;
        }
        if ("brand".equals(group)) {
            brand(sender, args, config);
            return;
        }
        if ("handshake".equals(group)) {
            if (args.length != 2) { usage(sender, "/lunarspoof handshake <on|off>"); return; }
            Boolean value = parseToggle(args[1]);
            if (value == null) { error(sender, "Use on or off."); return; }
            config.hideForgeHandshake = value;
            config.save();
            success(sender, "Forge/FML handshake hiding " + (value ? "enabled" : "disabled") + ".");
            return;
        }
        if ("icons".equals(group)) {
            if (args.length != 2) { usage(sender, "/lunarspoof icons <on|off>"); return; }
            Boolean value = parseToggle(args[1]);
            if (value == null) { error(sender, "Use on or off."); return; }
            config.showPeerIcons = value;
            config.save();
            if (!value) {
                org.afterlike.moonlight.peers.SubscriptionManager.disconnect();
            }
            success(sender, "Lunar peer icons " + (value ? "enabled (reconnect to activate Lunar-service lookup)" : "disabled; Lunar-service connection closed") + ".");
            return;
        }
        if ("rpc".equals(group)) {
            rpc(sender, args, config);
            return;
        }
        if ("version".equals(group)) {
            info(sender, "Lunar build: " + Moonlight.get().getLunarVersion());
            info(sender, "Current client brand: " + Moonlight.get().getClientBrand());
            return;
        }
        help(sender);
    }

    private static void brand(ICommandSender sender, String[] args, LunarSpoofConfig config) {
        if (args.length == 1 || "status".equalsIgnoreCase(args[1])) {
            info(sender, "Brand mode: " + config.brandMode + " | sent value: " + Moonlight.get().getClientBrand());
            info(sender, "Set with /lunarspoof brand <lunar|vanilla|forge|custom <text>>");
            return;
        }
        String mode = args[1].toLowerCase(java.util.Locale.ROOT);
        if (!Arrays.asList("lunar", "vanilla", "forge", "custom").contains(mode)) {
            error(sender, "Mode must be lunar, vanilla, forge, or custom.");
            return;
        }
        if ("custom".equals(mode)) {
            if (args.length < 3) { usage(sender, "/lunarspoof brand custom <text>"); return; }
            config.customBrand = LunarSpoofConfig.sanitizeBrand(join(args, 2));
            if (config.customBrand.isEmpty()) { error(sender, "Custom brand cannot be empty."); return; }
        }
        config.brandMode = mode;
        config.save();
        Moonlight.get().configureBrand(config.brandMode, config.customBrand);
        success(sender, "Client brand mode set to " + mode + ": " + Moonlight.get().getClientBrand()
                + ". Reconnect to apply it to the next connection.");
    }

    private static void rpc(ICommandSender sender, String[] args, LunarSpoofConfig config) {
        if (args.length == 1 || "status".equalsIgnoreCase(args[1])) {
            info(sender, "Discord RPC: " + (config.discordRpcEnabled ? "on" : "off"));
            info(sender, "Application ID configured: " + LunarSpoofConfig.isValidApplicationId(config.discordApplicationId));
            info(sender, "Use /lunarspoof rpc appid <your-discord-application-id>, then /lunarspoof rpc on.");
            return;
        }
        if ("on".equalsIgnoreCase(args[1])) {
            if (!LunarSpoofConfig.isValidApplicationId(config.discordApplicationId)) {
                error(sender, "Set a valid Discord application ID first: /lunarspoof rpc appid <id>");
                return;
            }
            config.discordRpcEnabled = true;
            config.save();
            DiscordPresenceManager.refresh();
            success(sender, "Discord Rich Presence enabled. Keep the Discord desktop app running.");
            return;
        }
        if ("off".equalsIgnoreCase(args[1])) {
            config.discordRpcEnabled = false;
            config.save();
            DiscordPresenceManager.refresh();
            success(sender, "Discord Rich Presence disabled.");
            return;
        }
        if ("appid".equalsIgnoreCase(args[1])) {
            if (args.length != 3 || !LunarSpoofConfig.isValidApplicationId(args[2])) {
                error(sender, "Usage: /lunarspoof rpc appid <15-22 digit application ID>");
                return;
            }
            config.discordApplicationId = args[2];
            config.save();
            DiscordPresenceManager.refresh();
            success(sender, "Discord application ID saved.");
            return;
        }
        if ("details".equalsIgnoreCase(args[1]) || "state".equalsIgnoreCase(args[1])) {
            if (args.length < 3) { usage(sender, "/lunarspoof rpc " + args[1] + " <text>"); return; }
            String text = LunarSpoofConfig.trimTo(join(args, 2), 128);
            if (text.isEmpty()) { error(sender, "Text cannot be empty."); return; }
            if ("details".equalsIgnoreCase(args[1])) config.discordDetails = text;
            else config.discordState = text;
            config.save();
            DiscordPresenceManager.updatePresence();
            success(sender, "Discord Rich Presence text updated.");
            return;
        }
        if ("image".equalsIgnoreCase(args[1])) {
            if (args.length != 3) { usage(sender, "/lunarspoof rpc image <asset-key|off>"); return; }
            config.discordLargeImageKey = "off".equalsIgnoreCase(args[2]) ? "" : args[2].trim();
            config.save();
            DiscordPresenceManager.updatePresence();
            success(sender, "Discord large image setting updated. Asset keys must exist in your Discord application.");
            return;
        }
        error(sender, "Unknown RPC option. Use /lunarspoof rpc.");
    }

    private static void status(ICommandSender sender, LunarSpoofConfig config) {
        info(sender, "Brand: " + config.brandMode + " (" + Moonlight.get().getClientBrand() + ")");
        info(sender, "Hide Forge/FML handshake: " + config.hideForgeHandshake);
        info(sender, "Show peer icons: " + config.showPeerIcons);
        info(sender, "Discord RPC: " + config.discordRpcEnabled);
    }

    private static void help(ICommandSender sender) {
        info(sender, "LunarSpoof — local client settings (not sent as chat).");
        info(sender, "/lunarspoof — open the ClickGUI; /lunarspoof gui — reopen it.");
        info(sender, "/lunarspoof status — show current settings.");
        info(sender, "/lunarspoof brand <lunar|vanilla|forge|custom <text>>");
        info(sender, "/lunarspoof handshake <on|off>  |  /lunarspoof icons <on|off>");
        info(sender, "/lunarspoof rpc <status|on|off|appid <id>|details <text>|state <text>|image <key|off>>");
        info(sender, "Discord RPC requires your own Discord Developer Application ID.");
    }

    private static Boolean parseToggle(String value) {
        if ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("off".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) return Boolean.FALSE;
        return null;
    }

    private static String join(String[] values, int start) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < values.length; i++) {
            if (i > start) result.append(' ');
            result.append(values[i]);
        }
        return result.toString();
    }

    private static void usage(ICommandSender sender, String message) { info(sender, message); }
    private static void info(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[LunarSpoof] " + EnumChatFormatting.WHITE + message));
    }
    private static void success(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[LunarSpoof] " + EnumChatFormatting.WHITE + message));
    }
    private static void error(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[LunarSpoof] " + EnumChatFormatting.WHITE + message));
    }
}
