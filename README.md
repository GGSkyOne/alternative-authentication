# Alternative Authentication

Alternative Authentication is a mod that allows you to use two or more third-party or custom authentication servers without interfering with Mojang's authentication servers. In addition, it also implements whitelist support for third-party authentication servers.

By default, there are two configured authentication servers in the config - [Mojang](https://wiki.vg/Mojang_API) and [Ely.by](https://docs.ely.by/en/api.html). You can add or remove as many as you want.

Requires `online-mode=true` in the server configuration for correct operation. Also it is recommended to set `enforce-secure-profile=false` to disable message signing that players joining via third-party providers do not have.

Keep in mind that this is only a server-side mod! And if you want to use third-party authentication on the client, you need to use a supported launcher or authlib-injector.

## Features

-   Convenient configuration of authentication servers.
-   Support for third-party authentication servers when joining the server.
-   Support for fetching additional properties for the player when joining server. Example of use: set the textures (skin and cape) for the player server-side so client can see player's texture without any mods.
-   Support for adding custom properties to the player when joining server.
-   Add or remove from the whitelist using third-party authentication servers.

## Joining the server

When a player connects to a server, authentication first occurs on Mojang servers, if the player cannot be authenticated there, then a check is made on <span>Ely.by</span> or other servers, depending on the configuration. You can change the order of checking servers in the configuration, for example, first check in <span>Ely.by</span>, and then in Mojang. If the player has not passed the check on any server, then the player is not allowed to the server. His username could not be verified.

## Whitelist

You can use the whitelist with this mod, it implements support for third-party authentication servers for adding to the whitelist.

When you add a player to the whitelist, the check first occurs on Mojang servers, if the player was not found, then the check is hapenning on <span>Ely.by</span> or other servers, depending on the configuration. You can change the order of checking servers in the configuration, for example, first check in <span>Ely.by</span>, and then in Mojang. If the player was not found on any of the servers, then he cannot be added to the whitelist.

### What if there are two people with the same username on multiple authentication servers?

Well, in this case, you can temporarily change the order of authentication servers or add the player to the whitelist yourself.

## Configuration

This is an example configuration file, located at `/config/alternative-auth.json`.

### Debug

If you change the value from false to true, then during authentication or whitelisting, the corresponding data will be logged to the console, which can be useful for troubleshooting. Disabled by default.

### Providers

Your authentication providers, or authentication servers. You can remove, add, or swap them to change the order of checking.

-   `name`. Display name for the provider.

-   `check_url`. URL for checking the player during authentication when joining the server.

-   `profiles_url`. URL for checking the player when adding or removing from the whitelist. Note that here must be used a URL for checking multiple usernames in an array, not just one username.

-   `property_url`. Not required. Additional URL for fetching custom properties (like skin and cape), if needed, for the player when joining server. You can use `{0}` to put player username in the request, or `{1}` to put player UUID.

-   `custom_properties`. Not required. Array of custom properties that will be applied to the player when player joins the server with this authentication provider.

```
{
    "debug": false,
    "providers": [
        {
            "name": "Mojang",
            "check_url": "https://sessionserver.mojang.com/session/minecraft/hasJoined",
            "profiles_url": "https://api.mojang.com/profiles/minecraft"
        },
        {
            "name": "Ely.by",
            "check_url": "https://authserver.ely.by/session/hasJoined",
            "profiles_url": "https://authserver.ely.by/api/profiles/minecraft",
            "property_url": "http://skinsystem.ely.by/textures/signed/{0}",
            "custom_properties": [
                {
                    "name": "your_custom_property",
                    "value": "but why are you asking?"
                }
            ]
        }
    ]
}

```

### FAQ

Answers to some questions.

#### Q: Backport?

A: Maybe.

#### Q: Update?

A: I will try to support the mod on the latest versions of the game, it may take some time.

#### Q: I have issue with the mod, the mod doesn't work correctly, etc.

A: [Open an issue](https://github.com/GGSkyOne/alternative-authentication/issues)
