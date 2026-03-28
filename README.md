# Alternative Authentication

Alternative Authentication is a server-side mod that allows multiple third-party or custom authentication servers to coexist alongside Mojang's. By default, two providers are configured: [Mojang](https://minecraft.wiki/w/Mojang_API) and [Ely.by](https://docs.ely.by/en/api.html). You can add, remove, or reorder them freely.

**Requirements:**

- `online-mode=true` in `server.properties`
- `enforce-secure-profile=false` is recommended, players joining via third-party providers don't have signed chat keys

> **Server-side only.** Players using a third-party auth provider on the client need a compatible launcher or injector.

## Features

- ⚙️ Configurable list of authentication providers.
- 🔐 Sequential provider fallback when a player joins the server
- 🔒 Optional fallback prevention: if a username exists on a provider but authentication fails, further providers are not tried.
- 👕 Player texture support (skin and cape) via third-party skin servers
- 📝 Whitelist support across all configured providers

## How It Works

### Joining the Server

When a player connects, each configured provider is tried in order until one successfully authenticates the session. If no provider succeeds, the player is denied access.

If [`preventFallbackIfPlayerExists`](#preventfallbackifplayerexists) is enabled and a player's username is found on a provider but authentication fails, no further providers are attempted. This prevents a user registered on a fallback service from impersonating an account on an earlier provider.

### Whitelist

When a player is added to the whitelist, each provider is queried in order until the username is resolved to a UUID. If the player is not found on any provider, they cannot be added. The order in which providers are checked is determined by the [`providers`](#providers) list in the config.

#### What if two players share the same username across providers?

By default, the first provider to resolve the username wins. If you need to whitelist a player whose username exists on a non-primary provider, temporarily reorder the [`providers`](#providers) list and add them manually with `/whitelist add`.

## Configuration

The config file is created automatically at `config/alternative-auth.json` on first launch.

### `configVersion`

The internal schema version. Managed automatically by the mod, do not edit this manually.

### `debugMode`

When `true`, detailed logs are written to the console during authentication and whitelist operations. Useful for troubleshooting. Disabled by default.

### `preventFallbackIfPlayerExists`

When `true`, stops the provider chain if a username is found on the current provider but authentication fails. Prevents username spoofing via fallback services, non-premium players with usernames unique to a fallback service are not affected. Disabled by default.

### `providers`

The ordered list of authentication providers. Each entry supports the following fields:

| Field         | Required | Description                                                                                                                 |
|---------------|----------|-----------------------------------------------------------------------------------------------------------------------------|
| `name`        | ✅        | Display name for the provider                                                                                               |
| `checkUrl`    | ✅        | Session validation endpoint, queried when a player joins the server                                                         |
| `profileUrl`  | ✅        | Single username to UUID lookup endpoint, used for whitelist operations                                                      |
| `profilesUrl` | ✅        | Batch username to UUID lookup endpoint                                                                                      |
| `propertyUrl` | ❌        | Endpoint for fetching player properties such as skin and cape. Use `{0}` for the player's username and `{1}` for their UUID |

```json
{
    "configVersion": 1,
    "debugMode": false,
    "preventFallbackIfPlayerExists": false,
    "providers": [
        {
            "name": "Mojang",
            "checkUrl": "https://sessionserver.mojang.com/session/minecraft/hasJoined",
            "profileUrl": "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
            "profilesUrl": "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname"
        },
        {
            "name": "Ely.by",
            "checkUrl": "https://authserver.ely.by/session/hasJoined",
            "profileUrl": "https://authserver.ely.by/api/users/profiles/minecraft/",
            "profilesUrl": "https://authserver.ely.by/api/profiles/minecraft",
            "propertyUrl": "http://skinsystem.ely.by/textures/signed/{0}"
        }
    ]
}
```

## FAQ

### Q: Can't join the server, "Invalid signature for profile public key"

A: Install [No Chat Reports](https://modrinth.com/mod/no-chat-reports) on your server. Installing it on the client is recommended but not required.

### Q: Is Forge or NeoForge supported?

A: NeoForge support is planned.

### Q: When will the mod be updated to the latest Minecraft version?

A: The mod targets the latest stable release and is updated as time allows. Snapshot support is not planned.

### Q: I found a bug, have a suggestion, or the mod isn't working correctly

A: Feel free to [open an issue on GitHub](https://github.com/GGSkyOne/alternative-authentication/issues)!