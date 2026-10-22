# LunarSpoof — Minecraft Forge 1.8.9

A client-side Forge coremod for **Minecraft Java 1.8.9 / Forge 11.15.1.2318**. It replaces the client-brand string with `vanilla`, registers the `Lunar-Client` custom-payload channel, and sends the public API UUID payload used by compatible servers.

This is a protocol-level compatibility mod, **not** the Lunar Client, and it does not bypass Lunar Client's protected-server anticheat or authenticate a player to Lunar services. Servers may still detect, reject, or prohibit this behavior; follow the rules of the server you join.

## Install

1. Install Minecraft Forge 1.8.9 (11.15.1.2318).
2. Put `LunarSpoof-Forge-1.8.9-1.0.0.jar` in the Minecraft `mods` folder.
3. Start the Forge 1.8.9 profile.

Use Java 8 for Minecraft 1.8.9.

## Build

The project uses ForgeGradle 2.1 and Gradle 2.14.1. With JDK 8 and Gradle 2.14.1 installed, run:

```bash
gradle build
```

The output JAR is written to `build/libs/`. The GitHub Actions workflow builds the project and publishes the JAR as a workflow artifact on each push.

## Credits and license

This is an independent 1.8.9 implementation informed by the public protocol behavior described in [leafypout/LunarClientSpoofer](https://github.com/leafypout/LunarClientSpoofer). That upstream README says its original project targeted 1.7.10 and does not bypass protected Lunar Client anticheat. This repository's implementation is covered by the MIT license in `LICENSE`.
