
# 🍃 Flowsense - Minecraft plugin for real-time donations from Saweria, Trakteer, and Tako.

**Minecraft plugin** for real-time donation system integration into the game using backend polling. Perfect for servers that want to display donations from Saweria, Trakteer, or Tako **directly into the server** with cool and automatic notifications.

## ✨ Features

- Real-time donation polling from backend
- Automatically handles authorization and updates
- Sends donation data directly to your Minecraft server
- Custom logging with `loggx` and message sending via `sendMessage`

## 📜 Commands

- `/tipflow reload` – Reloads the config and reconnects to the backend with a fresh session

## ⚙️ Setup

1. Drop the plugin `.jar` into your server's `plugins/` folder.
2. Start your server to generate `config.yml`.
3. Edit the config with your:
   - `token`
   - `provider` (1 = Saweria, 2 = Tako, 3 = Trakteer)
   - `prtoken`
4. Use `/tipflow reload` to apply changes and connect.

## 🧱 Dependency

- Bukkit/Spigot API
- Gson
- Adventure (MiniMessage)


## ✏️ Notes

- Each time `/tipflow reload` is called, the plugin will:
  - Exit the previous session (if any)
  - Reload `config.yml`
  - Re-authenticate to get a new `clientId`
  - Start polling for new donations

- The plugin uses long polling with a 29s timeout and will auto-refresh the client every request to avoid being flushed by the backend.
