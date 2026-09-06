# SalamTV

Android IPTV player for the SalamTV service — phones, tablets, and Android TV / Fire TV.

## Credits and licence

SalamTV is a fork of **[OwnTV](https://github.com/ahXN00/OwnTV)** by [@ahXN00](https://github.com/ahXN00),
licensed under the **GNU General Public License v3.0**. This fork keeps that licence: the full
corresponding source for every APK published under [Releases](../../releases) is in this repository
and in [salamtv-core](../../../salamtv-core), and you may use, study, modify and redistribute it
under the same terms. See [LICENSE](LICENSE).

The shared library module lives in its own repository, as it does upstream:
[**salamtv-core**](../../../salamtv-core) — a fork of
[OwnTV_Core](https://github.com/ahXN00/OwnTV_Core).

## What differs from upstream

**Subscriber sign-in instead of manual source entry.** Upstream OwnTV is a general player: you type a
server address and pick a playlist type. SalamTV asks for a username and a password only. The panel
answers with the host to use, and decides the channel set, the quality ceiling and the device limit —
the app is not asked to know any of it, and cannot be talked into ignoring it.

**Our own metadata endpoints.** Upstream ships a default TMDB Worker and an OpenSubtitles Worker run
by its maintainer. A fork that keeps them spends someone else's API quota on its own users and breaks
the day that maintainer retires them. Both now point at endpoints under our control, with the keys
held server-side and never in the APK.

**Update channel.** In-app updates come from this repository's releases.

## Build

```
./gradlew :app:assembleStandardRelease   # arm64-v8a + armeabi-v7a — real devices
./gradlew :app:assembleX86_64Debug       # emulator
```

ABI-split flavours: `standard` (ARM), `x86_64`, `x86`. Splitting them roughly halves what a user
downloads compared with one universal APK.

The build reads two values that are not in this repository:

| Property | Meaning | Default |
|---|---|---|
| `salamtv.loginUrl` | Sign-in endpoint; blank disables locked mode | the production endpoint |
| `owntv.keystore*` | Release signing | unsigned build |

Put them in `~/.gradle/gradle.properties` or pass them as environment variables. **Signing keys never
belong in a repository** — a leaked key lets anyone publish a signed update that installs silently on
every device that trusts it.

## Not in this repository

The subscriber panel is separate commercial software and is not distributable. This repository is the
client only.
