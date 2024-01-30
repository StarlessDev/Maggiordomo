# Maggiordomo
![License](https://img.shields.io/github/license/StarlessDev/Maggiordomo?style=for-the-badge&color=white)
![Jenkins](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci.starless.dev%2Fjob%2FMaggiordomo%2F&style=for-the-badge)
![Uptimerobot](https://img.shields.io/uptimerobot/status/m794788591-44f606edea995f3d5821536a?style=for-the-badge)

A bot created to manage and customize temporary rooms (and more) in your server discord completely for free, with ease and without any useless features.

## Invite
~~If you are not able to host the bot on your own, there is a public instance of Maggiordomo you can invite <a href="https://maggiordomo.starless.dev/invite" target="_blank">here</a>~~.

Unfortunately, the bot was not granted access to the **GUILD_MEMBERS** intent because, according to Discord support, Maggiordomo lacks a «unique, compelling, user-facing functionality» which uses this intent.
Without this intent:
- pinned rooms are not deleted when a user leaves
- members can only be loaded via their ids, so adding trusted and banned users by username does not work (according to my tests, please let me know if there is a solution)

For this reason, at the moment, Maggiordomo (despite being verified) is limited to 100 guilds and no other guilds can add the bot.
I will try to reapply for the intent when I have such a feature as described above or find a workaround not to use this intent.

## Features
- Support for **temporary** and **pinned** (aka *permanent*) rooms
- Room customization:
  - Name
  - Capacity
  - Whitelist and blacklist users
  - Kick users
- Support for multiple categories
- Filters: do not let users use offensive names for their rooms
- Manage all the rooms easily in a single dashboard
- Fully translatable: currently only available in English and Italian
- and more...

## Documentation
All of the documentation, invite link and uptime information is available on the new site <a href="https://maggiordomo.starless.dev" target="_blank">https://maggiordomo.starless.dev</a>.
