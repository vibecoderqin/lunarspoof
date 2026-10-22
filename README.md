# LunarSpoof — Forge 1.8.9

Client-side mod for Minecraft Java 1.8.9 / Forge 11.15.1.2318. It combines configurable client-brand reporting, optional Forge/FML handshake suppression, Lunar peer-icon support through the Lunar services protocol, and optional Discord Rich Presence.

This repository includes GPL-3.0-licensed code adapted from [zhgmx/Moonlight](https://github.com/zhgmx/Moonlight), which implements the Apollo protocol and Lunar peer discovery. The ClickGUI window layout and interaction model are inspired by [BleachHack Legacy](https://github.com/BleachDev/bleachhack-legacy/) (GPL-3.0), but the port only contains LunarSpoof settings controls and no BleachHack gameplay modules. The license is GPL-3.0; see `LICENSE`.

## Features

- Brand mode: `lunar`, `vanilla`, `forge`, or custom string.
- Optional FML handshake suppression; some Forge-only servers may refuse connections when enabled.
- Lunar peer detection and local tab-list icons where Lunar's services provide the required information.
- Optional Discord Rich Presence using your own Discord Developer Application ID. The default status avoids showing the server address.
- Graphical ClickGUI: run `/lunarspoof` (aliases `/lspoof`, `/ls`) to open the draggable, collapsible settings windows.
- Text command help: `/lunarspoof help`; use `/lunarspoof gui` to reopen the ClickGUI.

## Commands

- `/lunarspoof` — open the ClickGUI.
- `/lunarspoof help` — text command help.
- `/lunarspoof status` — show current settings.
- `/lunarspoof brand <lunar|vanilla|forge|custom <text>>` — choose the client brand sent on subsequent connections.
- `/lunarspoof handshake <on|off>` — toggle suppression of the Forge/FML handshake.
- `/lunarspoof icons <on|off>` — toggle local Lunar peer icons in the Tab List.
- `/lunarspoof rpc appid <ID>` — set your Discord Developer Application ID.
- `/lunarspoof rpc on|off` — enable/disable Rich Presence.
- `/lunarspoof rpc details <text>` and `/lunarspoof rpc state <text>` — change visible status text.
- `/lunarspoof rpc image <asset-key|off>` — set the large image key (the key must exist in your Discord application).

The ClickGUI has **Client Brand**, **Privacy**, and **Discord RPC** windows. Drag a title bar to move a window; right-click its title bar (or click the collapse marker) to collapse it. Input values are saved when the screen closes; feature toggles save immediately.

Settings persist in `.minecraft/config/lunarspoof.json`.

## Discord Rich Presence setup

1. Create a Discord application in the Discord Developer Portal and copy its Application ID.
2. Add a large image asset to that application (for example, key `lunar_logo`), or choose a key you already uploaded.
3. Start Minecraft and the Discord desktop app, then run `/lunarspoof rpc appid <ID>` and `/lunarspoof rpc on`.
4. Customize the two text fields with `/lunarspoof rpc details` and `/lunarspoof rpc state`.

The activity content and artwork belong to the Discord application you configure; it is not the official Lunar Client Discord application.

## Installation

1. Install Minecraft Forge 1.8.9 (11.15.1.2318).
2. Download the JAR from the latest successful [GitHub Actions build](https://github.com/vibecoderqin/lunarspoof/actions).
3. Put the JAR into `.minecraft/mods` and launch Forge 1.8.9 with Java 8.

## Privacy and limitations

The client brand is a client-reported string, not cryptographic proof of the running client. Lunar's own developer documentation says it can be spoofed: [Client Brand](https://lunarclient.dev/client-brand). A compatible server may display the selected brand, ignore it, or use additional checks.

Lunar peer icons require the mod to communicate with Lunar's authentication and WebSocket endpoints. This can associate your Minecraft UUID/username with Lunar services. If you do not want this network activity, do not use peer-icon functionality. The mod does not store your Minecraft session token in its config file. Discord Rich Presence communicates locally with the Discord desktop client.

Icon behavior is not guaranteed: official Lunar Client can use its own service-side and client-side checks, and server type/settings still affect display. This mod is not the official Lunar Client and does not bypass protected-server anticheat. Follow each server's rules.

## Build from source

Requires JDK 8 and Gradle 8.8 installed.

```bash
gradle spotlessApply build
```

The JAR is written to `build/libs/`. GitHub Actions builds and uploads it as an artifact on every push to `main`.
