# LunarSpoof 1.1.1 Candidate — Static Security Audit

**Audit date:** 2026-10-26  
**Scope:** Source/configuration review of the `audit/fixes-1.1.1` branch against its `main` base, with a review of recent GitHub Actions history.  
**Runtime testing:** Not performed in this environment. The branch must pass CI and still needs Forge/server integration testing before being treated as a verified release.

## Executive summary

No critical vulnerability was confirmed in the reviewed source. The review found lifecycle and resilience issues in Lunar peer connections and Discord RPC, plus input/configuration robustness gaps. This branch contains minimal fixes and documents current network behavior. The compatibility-sensitive defaults and handshake-suppression logic were not silently changed.

## Confirmed findings and changes

| Severity | Location | Finding | Change |
| --- | --- | --- | --- |
| Major | `src/main/java/org/afterlike/moonlight/peers/SubscriptionManager.java` (connect/disconnect and tab-list sync) | The initial JWT argument was ignored, creating an unnecessary authentication round trip. An asynchronous reconnect could race with privacy-toggle or account changes. The reconnect callback also touched a non-thread-safe set from a Netty thread. | Reuse the initial JWT exactly once; fetch a fresh JWT only for reconnects; refuse connect when icons are disabled; invalidate old WS references on disconnect; clear volatile auth state; use a concurrent set and guard reconnect callbacks against stale clients. |
| Major | `src/main/java/org/afterlike/moonlight/peers/WSClient.java` (connect loop and WebSocket handler) | No explicit timeout covered the WebSocket HTTP upgrade after TLS; malformed/oversized frames could cause disproportionate allocations; pending request IDs survived a connection attempt; writes did not validate active channel state. | Add a 15-second WebSocket upgrade timeout, cap aggregated WebSocket frames at 1 MiB, clear pending login IDs for each connection attempt, reject empty JWTs and stop after an in-flight auth if disconnected, reset backoff after a successful connection, and check the channel before writes. |
| Major | `src/main/java/org/afterlike/moonlight/rpc/DiscordPresenceManager.java` (lifecycle) | Presence updates and shutdown could run synchronously on the Minecraft/GUI thread. A failure while clearing activity could skip the actual RPC shutdown. Client init/refresh/stop operations could race. | Serialize RPC operations on a daemon worker; separate clearing presence from shutdown so shutdown is still attempted if clearing fails; verify the active client/Application ID after initialization; bound shutdown-hook waiting to three seconds. |
| Minor | `src/main/java/org/afterlike/moonlight/platform/ForgeModEntry.java` (async auth callback) | An authentication result could be accepted after the Minecraft account/session changed while the request was pending. | Compare the live UUID to the UUID captured when auth started and discard stale results. |
| Minor | `src/main/java/org/afterlike/moonlight/config/LunarSpoofConfig.java` (load/sanitize) | A malformed JSON config could be overwritten with defaults, losing the user's recoverable settings. Custom-brand control characters were not normalized. | Preserve an unreadable config as `lunarspoof.json.corrupt` before writing defaults, and strip control characters/truncate custom brand strings. |
| Minor | `src/main/java/org/afterlike/moonlight/command/LunarSpoofCommand.java`, `LunarSpoofClickGui.java`, `Moonlight.java` | Custom brand sanitation was inconsistent across UI, command and runtime paths; the command image key path did not consistently cap its length. | Sanitize custom brand at config, GUI, command and runtime boundaries; cap command-provided image asset keys at 256 characters. |
| Minor | `build.gradle.kts`, `ForgeModEntry.java`, `.github/workflows/build.yml` | The project version was hardcoded, and the build workflow uploaded an artifact but did not publish a tag-triggered release. | Derive the version from a semantic version tag (fallback `1.1.1`), expose the generated build version in Forge metadata, and add a tag-only release job with `contents: write` scoped to that job. |

## Privacy and security checks

- **Minecraft session token:** no persistence of the Minecraft session token to `lunarspoof.json` was found in reviewed code. The normal Minecraft `sessionService.joinServer(..., session.getToken(), ...)` challenge is used by Lunar authentication; the raw session token is not serialized into the Lunar WebSocket handshake. No direct logging of the raw token was found. The short-lived Lunar JWT is held in runtime memory, not intentionally written to disk, and is cleared on peer-service disconnect.
- **Lunar peer data:** when peer icons are enabled, the client authenticates to `authenticator.lunarclientprod.com` and connects to `websocket.lunarclientprod.com`. The protocol includes local player identity (UUID and username), platform/client metadata, and tab-list player UUIDs for icon lookup.
- **Version metadata:** when the cache is missing or stale, `VersionFetcher` calls `api.lunarclientprod.com/launcher/launch`; request metadata includes OS name/release, architecture, game/launcher version fields and an installation identifier. This is independent of the peer-icon toggle.
- **Discord RPC:** disabled by default, uses the user-supplied Discord Application ID and local Discord desktop IPC. Default activity does not include the multiplayer server address. Discord itself may transmit activity according to its own behavior/settings.
- **User input:** Application ID is validated as 15–22 ASCII digits; presence details/state and image keys are length-limited; custom brands have control characters removed.
- **Client command access:** `LunarSpoofCommand.getRequiredPermissionLevel()` returns 0, appropriate for this client-only command.
- **Java compatibility:** Gradle source/target and toolchain target Java 8. The changes use Java 8 language/runtime APIs.

## Open risks / follow-up

### Forge handshake suppression requires integration testing

`NetworkDispatcherMixin.java` intentionally aborts the Forge handshake as vanilla when the option is enabled. That can cause Forge-only servers to reject the connection, which is documented in README. I did not rewrite the handshake interception because the precise behavior depends on Forge 1.8.9's state machine and requires testing against both vanilla and Forge-only servers. Recommended test matrix: vanilla server with option on/off, Forge server with option on/off, single-player, disconnect/reconnect, and servers that send unexpected custom payloads.

### Network integrations should become explicitly opt-in

The peer-icon setting currently defaults to enabled for compatibility, and the version-metadata updater still runs independently of that toggle. The README now describes the hosts and data categories, but a single disclosure is not equivalent to explicit consent. See [Issue #1](https://github.com/vibecoderqin/lunarspoof/issues/1) for a separate opt-in/defaults design proposal. The current branch does not flip defaults and surprise existing users.

### Version fetch may block cold startup

When no cached Lunar git commit exists, `ForgeModEntry.init` currently calls `VersionFetcher.fetch()` synchronously. Moving it off-thread needs a safe strategy for the Apollo handshake so startup can remain responsive without sending incomplete metadata. See [Issue #2](https://github.com/vibecoderqin/lunarspoof/issues/2).

## Build / release status

- GitHub Actions run [#83](https://github.com/vibecoderqin/lunarspoof/actions/runs/39822027774) for the existing `main` branch completed successfully.
- Historical failed runs included a Gradle Kotlin DSL error in `build.gradle.kts` where `RemapJarTask.classpath.setFrom` was inaccessible; that code was no longer present in the inspected current `main` tree.
- This audit branch has not yet been validated by a fresh CI run. Static inspection alone is not evidence that the resulting JAR builds or works on a live Forge server.
- **Version 1.1.1 is a candidate, not yet a published release.** After the PR is reviewed/merged, pushing tag `v1.1.1` runs the build and the restricted release job attaches the remapped `LunarSpoof-1.1.1-forge.jar`. Do not publish the candidate until PR CI passes and runtime smoke tests are completed.
