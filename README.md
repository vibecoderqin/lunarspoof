# LunarSpoof — Forge 1.8.9

LunarSpoof is a client-side Forge mod for Minecraft Java 1.8.9 / Forge 11.15.1.2318. It combines configurable client-brand reporting, optional Forge/FML handshake suppression, Lunar peer icons through Lunar services, and optional Discord Rich Presence.

This repository includes GPL-3.0-licensed code adapted from [zhgmx/Moonlight](https://github.com/zhgmx/Moonlight), which implements Apollo and Lunar peer discovery. The ClickGUI layout and interaction model are inspired by [BleachHack Legacy](https://github.com/BleachDev/bleachhack-legacy/) (GPL-3.0); this port contains LunarSpoof settings controls, not BleachHack gameplay modules. See [LICENSE](LICENSE).

## Features

- **Client brand:** `lunar`, `vanilla`, `forge`, or a custom string.
- **Optional FML handshake suppression:** may prevent connections to Forge-only servers when enabled.
- **Lunar peer icons:** display locally rendered icons for players in the tab list when Lunar services return the necessary data.
- **Optional Discord Rich Presence:** uses your own Discord Developer Application ID. The default status does not include a server address.
- **ClickGUI:** run `/lunarspoof` (aliases `/lspoof`, `/ls`) to open draggable, collapsible settings windows.
- **Client-only commands:** settings commands do not require server operator permissions and their arguments are not sent as chat messages.

## Installation

1. Install Minecraft Forge 1.8.9 (11.15.1.2318).
2. Download `LunarSpoof-<version>-forge.jar` from the [latest GitHub Release](https://github.com/vibecoderqin/lunarspoof/releases/latest).
3. Put the JAR into `.minecraft/mods` and launch Forge 1.8.9.

Do not put the `without-deps` or intermediate JARs into the mods folder. Use the asset with the `-forge.jar` classifier.

## Commands

- `/lunarspoof` or `/lunarspoof gui` — open the ClickGUI.
- `/lunarspoof help` — list commands.
- `/lunarspoof status` — show current settings.
- `/lunarspoof brand <lunar|vanilla|forge|custom <text>>` — select the client brand sent on subsequent connections.
- `/lunarspoof handshake <on|off>` — toggle Forge/FML handshake suppression.
- `/lunarspoof icons <on|off>` — toggle Lunar peer icons and the related Lunar service connection.
- `/lunarspoof rpc appid <ID>` — set your Discord Developer Application ID (15–22 digits).
- `/lunarspoof rpc on|off` — enable or disable Rich Presence.
- `/lunarspoof rpc details <text>` and `/lunarspoof rpc state <text>` — set visible status text.
- `/lunarspoof rpc image <asset-key|off>` — set the large image key; the key must exist in your Discord application.

The ClickGUI includes **Client Brand**, **Privacy**, and **Discord RPC** windows. Drag a title bar to move a window; right-click its title bar or use the collapse marker to collapse it. Text fields are saved when the screen closes; feature toggles save immediately.

Settings are stored in `.minecraft/config/lunarspoof.json`. If this file exists but cannot be parsed, LunarSpoof preserves a `.corrupt` backup before writing defaults.

## Discord Rich Presence setup

1. Create an application in the [Discord Developer Portal](https://discord.com/developers/applications) and copy its Application ID.
2. Add a large image asset to that application, or choose an existing asset key.
3. Start Minecraft and the Discord desktop app, then run `/lunarspoof rpc appid <ID>` and `/lunarspoof rpc on`.
4. Customize the activity using `/lunarspoof rpc details <text>` and `/lunarspoof rpc state <text>`.

Rich Presence is disabled by default. The activity text and artwork belong to the Discord application you configure; the mod does not use the official Lunar Client Discord Application ID. The mod communicates with the local Discord desktop client through Discord RPC. Discord may then transmit that activity as part of its own service. The default activity does not publish the multiplayer server address.

## Network activity and privacy

LunarSpoof does not save the Minecraft session token in its config and does not intentionally log it. However, the optional integrations do make network requests:

- **Lunar version metadata:** when the local version cache is missing or older than 24 hours, `VersionFetcher` contacts `api.lunarclientprod.com/launcher/launch`. The request includes operating-system name, architecture, OS release, Minecraft/launcher version fields, and a generated `installation_id`. This metadata refresh is separate from the peer-icon toggle. It does not include the Minecraft username, UUID, or server address.
- **Peer icons:** peer icons are enabled by default for compatibility. When enabled, the mod authenticates through `authenticator.lunarclientprod.com` and opens a WebSocket to `websocket.lunarclientprod.com`. The Lunar protocol sends the current player's UUID and username, client/platform metadata, and UUIDs of players in the current tab list for icon lookup. Minecraft's session service is used for its authentication challenge; the raw Minecraft session token is not serialized into the Lunar WebSocket handshake by this mod.
- **Discord RPC:** when explicitly enabled, the mod sends the configured activity text and image key to the local Discord desktop client using your Application ID. No multiplayer server address is included in the default activity.

Use `/lunarspoof icons off` or the Privacy window to stop the peer-service connection and clear the local peer cache. The version-metadata refresh is independent of that toggle. If you want to prevent all Lunar service requests, disable peer icons and remove network access for LunarSpoof/Minecraft; the current version-metadata updater does not yet have a separate setting.

The client brand is an unauthenticated, client-reported string, not cryptographic proof of the running client. Lunar's documentation also notes that client brand can be spoofed: [Client Brand](https://lunarclient.dev/client-brand). A server may display the selected brand, ignore it, or apply additional checks.

Handshake suppression is experimental. It intentionally prevents normal Forge/FML negotiation and may cause Forge-only servers to reject or disconnect the client. Disable it when joining servers that require Forge networking. Icon behavior also depends on Lunar services and the server's configuration. LunarSpoof is not the official Lunar Client and does not bypass protected-server anticheat. Follow each server's rules.

## Build from source

Requires Gradle 8.8 and a Java 8 toolchain. The CI build runs Gradle on Java 17 and compiles the mod for Java 8.

```bash
gradle spotlessApply build
```

The distributable remapped JAR is written to `build/libs/`. Push a version tag such as `v1.1.1` to run the build and publish the `-forge.jar` file as a GitHub Release. Every push to `main` and every pull request also runs the build workflow; successful jobs upload a temporary build artifact.

## Compatibility and license

- Minecraft Java 1.8.9
- Forge 11.15.1.2318
- Java 8 runtime
- GPL-3.0; see [LICENSE](LICENSE)
