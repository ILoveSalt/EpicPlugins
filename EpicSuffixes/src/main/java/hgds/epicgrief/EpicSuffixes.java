package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class EpicSuffixes extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String PERMISSION_FILE_NAME = "permission.yml";
    private static final String SUFFIXES_FILE_NAME = "suffixes.yml";
    private static final String ADMIN_PERMISSION = "epicsuffixes.admin";
    private static final String USE_PERMISSION = "epicsuffixes.use";
    private static final String CUSTOM_PERMISSION = "epicsuffixes.custom";
    private static final String GIVE_PERMISSION = "epicsuffixes.give";
    private static final String RELOAD_PERMISSION = "epicsuffixes.reload";
    private static final String BYPASS_PERMISSION = "epicsuffixes.bypass";
    private static final Method LEGACY_MATCH_MATERIAL = findLegacyMatchMaterialMethod();

    private final Set<UUID> waitingForTitleInput = new HashSet<>();
    private File permissionFile;
    private File suffixesFile;
    private YamlConfiguration permissionStorage;
    private YamlConfiguration customSuffixes;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            ensureStorage();
            reloadStorage();
            registerCommand("suffix");
            registerCommand("suffixcustom");
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("Loaded " + getRegisteredSuffixes().size() + " suffixes.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "EpicSuffixes could not be enabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        waitingForTitleInput.clear();
        saveStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("suffix")) {
            handleSuffixCommand(sender, args);
            return true;
        }
        if (commandName.equals("suffixcustom")) {
            handleSuffixCustomCommand(sender, args);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("suffix")) {
            return completeSuffixCommand(sender, args);
        }
        if (commandName.equals("suffixcustom")) {
            return completeSuffixCustomCommand(sender, args);
        }
        return Collections.emptyList();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof SuffixInventoryHolder suffixHolder)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topInventory.getSize()) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GuiAction action = suffixHolder.actions.get(event.getRawSlot());
        if (action == null) {
            return;
        }

        if (action instanceof SuffixAction suffixAction) {
            applySuffix(player, suffixAction.suffix);
            sendConfiguredMessage(player, "set-suffix", Map.of(
                    "suffix", color(suffixAction.suffix),
                    "formatted", color(suffixAction.suffix)
            ));
            return;
        }

        if (action instanceof StaticItemAction staticAction) {
            handleStaticItemClick(player, suffixHolder, staticAction);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!waitingForTitleInput.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(this, () -> completeTitleInput(player, message));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        waitingForTitleInput.remove(event.getPlayer().getUniqueId());
    }

    private void handleSuffixCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            if (!(sender instanceof Player player)) {
                sendUsage(sender, "/suffix open <ник>");
                return;
            }
            openSuffixInventory(player, 0);
            return;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "give", "add" -> handleGiveSuffix(sender, args);
            case "set" -> handleSetExistingSuffix(sender, args);
            default -> {
                if (sender instanceof Player player) {
                    openSuffixInventory(player, 0);
                } else {
                    sendUsage(sender, "/suffix reload");
                }
            }
        }
    }

    private void handleSuffixCustomCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendCustomHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "set", "create" -> handleCreateCustomSuffix(sender, args);
            case "give", "add" -> handleGiveCustomCount(sender, args);
            case "count", "check" -> handleCheckCustomCount(sender, args);
            case "cancel", "stop" -> handleCancelTitleInput(sender);
            case "reload" -> handleReload(sender);
            default -> sendCustomHelp(sender);
        }
    }

    private void handleCreateCustomSuffix(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendUsage(sender, "/suffixcustom set <титул>");
            return;
        }
        if (!hasAccess(player, CUSTOM_PERMISSION)) {
            sendConfiguredMessage(player, "no-perms", Map.of());
            return;
        }
        if (args.length < 2) {
            beginTitleInput(player);
            return;
        }

        String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        createCustomSuffix(player, input, false);
    }

    private void handleSetExistingSuffix(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendUsage(sender, "/suffix set <титул>");
            return;
        }
        if (!hasAccess(player, USE_PERMISSION)) {
            sendConfiguredMessage(player, "no-perms", Map.of());
            return;
        }
        if (args.length < 2) {
            sendUsage(player, "/suffix set <титул>");
            return;
        }

        String requestedSuffix = normalizeSuffixKey(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        String ownedSuffix = findOwnedSuffix(player, requestedSuffix);
        if (ownedSuffix == null) {
            sendConfiguredMessage(player, "no-perms", Map.of());
            return;
        }

        applySuffix(player, ownedSuffix);
        sendConfiguredMessage(player, "set-suffix", Map.of(
                "suffix", color(ownedSuffix),
                "formatted", color(ownedSuffix)
        ));
    }

    private void handleGiveSuffix(CommandSender sender, String[] args) {
        if (!hasAccess(sender, GIVE_PERMISSION)) {
            sendConfiguredMessage(sender, "no-perms", Map.of());
            return;
        }
        if (args.length < 3) {
            sendUsage(sender, "/suffix give <ник> <титул>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        String suffix = normalizeSuffixKey(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        if (!isValidSuffixInput(sender, suffix)) {
            return;
        }

        addSuffixOwner(suffix, targetName);
        saveStorage();

        Map<String, String> variables = Map.of(
                "player", targetName,
                "suffix", color(suffix),
                "formatted", color(suffix)
        );
        send(sender, getConfig().getString("messages.give-suffix", "&6&lКАСТОМИЗАЦИЯ: &fВы выдали игроку &6&l%player% &fтитул &f&l➲ %suffix%"), variables);
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            sendConfiguredMessage(onlineTarget, "received-suffix", variables);
        }
    }

    private void handleGiveCustomCount(CommandSender sender, String[] args) {
        if (!hasAccess(sender, GIVE_PERMISSION)) {
            sendConfiguredMessage(sender, "no-perms", Map.of());
            return;
        }
        if (args.length < 3) {
            sendUsage(sender, "/suffixcustom give <ник> <количество>");
            return;
        }

        int count = parsePositiveInt(args[2]);
        if (count <= 0) {
            sendUsage(sender, "/suffixcustom give <ник> <количество>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String targetName = target.getName() == null ? args[1] : target.getName();
        addCustomCount(target.getUniqueId(), targetName, count);
        saveStorage();

        Map<String, String> variables = Map.of(
                "player", targetName,
                "count", String.valueOf(count)
        );
        sendConfiguredMessage(sender, "give-count", variables);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            sendConfiguredMessage(onlineTarget, "received-count", variables);
        }
    }

    private void handleCheckCustomCount(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            if (!hasAccess(sender, GIVE_PERMISSION)) {
                sendConfiguredMessage(sender, "no-perms", Map.of());
                return;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sendUsage(sender, "/suffixcustom count <ник>");
            return;
        }

        String targetName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        int count = getCustomCount(target.getUniqueId(), targetName);
        send(sender, getConfig().getString("messages.count", "&6&lКАСТОМИЗАЦИЯ: &fДоступно созданий титула &f&l➲ &6&l%count%"), Map.of(
                "player", targetName,
                "count", String.valueOf(count)
        ));
    }

    private void handleCancelTitleInput(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (waitingForTitleInput.remove(player.getUniqueId())) {
            sendConfiguredMessage(player, "input-cancelled", Map.of());
        }
    }

    private void handleReload(CommandSender sender) {
        if (!hasAccess(sender, RELOAD_PERMISSION)) {
            sendConfiguredMessage(sender, "no-perms", Map.of());
            return;
        }

        saveStorage();
        reloadConfig();
        reloadStorage();
        waitingForTitleInput.clear();
        sendConfiguredMessage(sender, "reload-success", Map.of());
    }

    private void beginTitleInput(Player player) {
        if (!hasAccess(player, CUSTOM_PERMISSION)) {
            sendConfiguredMessage(player, "no-perms", Map.of());
            return;
        }
        if (!canCreateWithoutCount(player) && getCustomCount(player) <= 0) {
            sendConfiguredMessage(player, "expired-count", Map.of());
            return;
        }

        waitingForTitleInput.add(player.getUniqueId());
        player.closeInventory();
        sendConfiguredMessage(player, "enter-title", Map.of("count", String.valueOf(getCustomCount(player))));
    }

    private void completeTitleInput(Player player, String message) {
        if (!player.isOnline()) {
            waitingForTitleInput.remove(player.getUniqueId());
            return;
        }

        String normalized = message == null ? "" : message.trim();
        if (normalized.equalsIgnoreCase("cancel") || normalized.equalsIgnoreCase("отмена")) {
            waitingForTitleInput.remove(player.getUniqueId());
            sendConfiguredMessage(player, "input-cancelled", Map.of());
            return;
        }

        CreateResult result = createCustomSuffix(player, message, true);
        if (result != CreateResult.INVALID) {
            waitingForTitleInput.remove(player.getUniqueId());
        }
    }

    private CreateResult createCustomSuffix(Player player, String input, boolean fromChat) {
        if (!hasAccess(player, CUSTOM_PERMISSION)) {
            sendConfiguredMessage(player, "no-perms", Map.of());
            return CreateResult.DONE;
        }

        String suffix = normalizeSuffixKey(input);
        if (!isValidSuffixInput(player, suffix)) {
            return fromChat ? CreateResult.INVALID : CreateResult.DONE;
        }

        boolean alreadyOwned = ownsSuffix(player, suffix);
        if (!alreadyOwned && !canCreateWithoutCount(player)) {
            int count = getCustomCount(player);
            if (count <= 0) {
                sendConfiguredMessage(player, "expired-count", Map.of());
                return CreateResult.DONE;
            }
            setCustomCount(player.getUniqueId(), player.getName(), count - 1);
            saveStorage();
        }

        if (!alreadyOwned) {
            addSuffixOwner(suffix, player.getName());
            saveStorage();
        }

        applySuffix(player, suffix);
        sendConfiguredMessage(player, alreadyOwned ? "set-suffix" : "custom-suffix-set", Map.of(
                "suffix", color(suffix),
                "formatted", color(suffix),
                "count", String.valueOf(getCustomCount(player))
        ));
        return CreateResult.DONE;
    }

    private boolean isValidSuffixInput(CommandSender sender, String suffix) {
        if (suffix == null || suffix.isBlank() || visibleLength(suffix) <= 0) {
            sendConfiguredMessage(sender, "incorrect-symbols", Map.of());
            return false;
        }

        int maxLength = Math.max(1, getConfig().getInt("settings.max-length", 11));
        if (visibleLength(suffix) > maxLength) {
            sendConfiguredMessage(sender, "max-length-error", Map.of(
                    "max", String.valueOf(maxLength),
                    "suffix", color(suffix),
                    "formatted", color(suffix)
            ));
            return false;
        }

        if (containsForbiddenCommandCharacters(suffix)) {
            sendConfiguredMessage(sender, "incorrect-symbols", Map.of());
            return false;
        }

        if (getConfig().getBoolean("settings.check-chars", false) && !hasOnlyAllowedVisibleCharacters(suffix)) {
            sendConfiguredMessage(sender, "incorrect-symbols", Map.of());
            return false;
        }

        return true;
    }

    private boolean containsForbiddenCommandCharacters(String suffix) {
        return suffix.indexOf('"') >= 0
                || suffix.indexOf('\n') >= 0
                || suffix.indexOf('\r') >= 0
                || suffix.indexOf('.') >= 0;
    }

    private boolean hasOnlyAllowedVisibleCharacters(String suffix) {
        String visible = ChatColor.stripColor(color(suffix));
        return visible != null && visible.matches("[\\p{L}\\p{N}_ \\-]+");
    }

    private int visibleLength(String suffix) {
        String visible = ChatColor.stripColor(color(suffix));
        return visible == null ? 0 : visible.length();
    }

    private void applySuffix(Player player, String suffix) {
        String formatted = color(suffix);
        Map<String, String> variables = Map.of(
                "player", player.getName(),
                "suffix", formatted,
                "formatted", formatted,
                "raw_suffix", suffix
        );

        for (String command : getConfig().getStringList("settings.command-give")) {
            dispatchConsoleCommand(replaceVariables(command, variables));
        }
    }

    private void openSuffixInventory(Player player, int requestedPage) {
        if (!hasAccess(player, USE_PERMISSION)) {
            sendConfiguredMessage(player, "no-perms", Map.of());
            return;
        }

        List<String> suffixes = getOwnedSuffixes(player);
        List<Integer> suffixSlots = configuredSuffixSlots();
        int slotsPerPage = Math.max(1, suffixSlots.size());
        int maxPage = suffixes.isEmpty() ? 0 : (suffixes.size() - 1) / slotsPerPage;
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        int size = inventorySize();

        SuffixInventoryHolder holder = new SuffixInventoryHolder(page, maxPage);
        Inventory inventory = Bukkit.createInventory(holder, size, color(getConfig().getString("inventory.title", "&0&lЛИЧНЫЕ ТИТУЛЫ")));
        holder.inventory = inventory;

        placeStaticItems(inventory, holder);
        if (suffixes.isEmpty()) {
            placeEmptyItem(inventory);
        } else {
            int from = page * slotsPerPage;
            for (int index = 0; index < suffixSlots.size(); index++) {
                int suffixIndex = from + index;
                if (suffixIndex >= suffixes.size()) {
                    break;
                }

                int slot = suffixSlots.get(index);
                if (!isInventorySlot(slot, inventory.getSize())) {
                    continue;
                }
                String suffix = suffixes.get(suffixIndex);
                inventory.setItem(slot, buildConfiguredItem("inventory.suffix-item", Map.of(
                        "suffix", suffix,
                        "formatted", color(suffix),
                        "page", String.valueOf(page + 1),
                        "max_page", String.valueOf(maxPage + 1)
                )));
                holder.actions.put(slot, new SuffixAction(suffix));
            }
        }

        player.openInventory(inventory);
    }

    private void placeStaticItems(Inventory inventory, SuffixInventoryHolder holder) {
        ConfigurationSection items = getConfig().getConfigurationSection("inventory.items");
        if (items == null) {
            return;
        }

        for (String key : items.getKeys(false)) {
            String path = "inventory.items." + key;
            ConfigurationSection section = getConfig().getConfigurationSection(path);
            if (section == null) {
                continue;
            }
            ItemStack item = buildConfiguredItem(path, Map.of(
                    "page", String.valueOf(holder.page + 1),
                    "max_page", String.valueOf(holder.maxPage + 1)
            ));
            StaticItemAction action = StaticItemAction.from(section);
            for (int slot : parseSlots(section.get("slot"))) {
                if (!isInventorySlot(slot, inventory.getSize())) {
                    continue;
                }
                inventory.setItem(slot, item);
                holder.actions.put(slot, action);
            }
        }
    }

    private void placeEmptyItem(Inventory inventory) {
        ConfigurationSection section = getConfig().getConfigurationSection("inventory.empty-item");
        if (section == null) {
            return;
        }

        ItemStack item = buildConfiguredItem("inventory.empty-item", Map.of());
        for (int slot : parseSlots(section.get("slot"))) {
            if (isInventorySlot(slot, inventory.getSize())) {
                inventory.setItem(slot, item);
            }
        }
    }

    private void handleStaticItemClick(Player player, SuffixInventoryHolder holder, StaticItemAction action) {
        if (action.createTitle) {
            beginTitleInput(player);
            return;
        }

        if (action.nextItem) {
            if (holder.page < holder.maxPage) {
                openSuffixInventory(player, holder.page + 1);
            }
            return;
        }

        if (action.backItem) {
            if (holder.page > 0) {
                openSuffixInventory(player, holder.page - 1);
                return;
            }
            for (String command : action.commandsIfFirstPage) {
                dispatchConsoleCommand(replaceVariables(command, Map.of("player", player.getName())));
            }
            return;
        }

        for (String command : action.commands) {
            dispatchConsoleCommand(replaceVariables(command, Map.of("player", player.getName())));
        }
        for (String message : action.messages) {
            send(player, message, Map.of("player", player.getName()));
        }
        if (action.close) {
            player.closeInventory();
        }
    }

    private ItemStack buildConfiguredItem(String path, Map<String, String> variables) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) {
            return new ItemStack(Material.STONE);
        }

        String materialName = section.getString("material", "STONE");
        int data = section.getInt("data", 0);
        Material material = resolveMaterial(materialName, data);
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, section.getInt("amount", 1))));

        if (material == Material.PLAYER_HEAD) {
            applyHeadTexture(item, section.getString("skull", ""));
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String display = replaceVariables(section.getString("display", ""), variables);
        if (!display.isBlank()) {
            meta.setDisplayName(color(display));
        }

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(color(replaceVariables(line, variables)));
        }
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void applyHeadTexture(ItemStack item, String encodedTexture) {
        if (encodedTexture == null || encodedTexture.isBlank() || !(item.getItemMeta() instanceof SkullMeta meta)) {
            return;
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(encodedTexture), StandardCharsets.UTF_8);
            String skinUrl = skinUrlFromTextureJson(decoded);
            if (skinUrl.isBlank()) {
                return;
            }

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(encodedTexture.getBytes(StandardCharsets.UTF_8)));
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(skinUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            item.setItemMeta(meta);
        } catch (Exception exception) {
            getLogger().fine("Could not apply configured skull texture.");
        }
    }

    private String skinUrlFromTextureJson(String decodedTexture) {
        int urlKeyIndex = decodedTexture.indexOf("\"url\"");
        if (urlKeyIndex < 0) {
            return "";
        }
        int colonIndex = decodedTexture.indexOf(':', urlKeyIndex);
        if (colonIndex < 0) {
            return "";
        }
        int firstQuoteIndex = decodedTexture.indexOf('"', colonIndex + 1);
        if (firstQuoteIndex < 0) {
            return "";
        }
        int secondQuoteIndex = decodedTexture.indexOf('"', firstQuoteIndex + 1);
        if (secondQuoteIndex < 0) {
            return "";
        }
        return decodedTexture.substring(firstQuoteIndex + 1, secondQuoteIndex);
    }

    private Material resolveMaterial(String configuredName, int data) {
        String name = configuredName == null ? "STONE" : configuredName.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (name.equals("SKULL_ITEM") || name.equals("PLAYER_SKULL") || name.equals("HEAD")) {
            name = "PLAYER_HEAD";
        } else if (name.equals("STAINED_GLASS_PANE")) {
            name = switch (data) {
                case 1 -> "ORANGE_STAINED_GLASS_PANE";
                case 4 -> "YELLOW_STAINED_GLASS_PANE";
                case 15 -> "BLACK_STAINED_GLASS_PANE";
                default -> "WHITE_STAINED_GLASS_PANE";
            };
        } else if (name.equals("STAINED_GLASS")) {
            name = switch (data) {
                case 1 -> "ORANGE_STAINED_GLASS";
                case 4 -> "YELLOW_STAINED_GLASS";
                case 15 -> "BLACK_STAINED_GLASS";
                default -> "WHITE_STAINED_GLASS";
            };
        }

        Material material = Material.matchMaterial(name);
        if (material == null && LEGACY_MATCH_MATERIAL != null) {
            try {
                material = (Material) LEGACY_MATCH_MATERIAL.invoke(null, name, true);
            } catch (ReflectiveOperationException ignored) {
                material = null;
            }
        }
        return material == null ? Material.STONE : material;
    }

    private List<String> getOwnedSuffixes(Player player) {
        List<String> suffixes = new ArrayList<>();
        addOwnedSuffixes(suffixes, getConfig().getConfigurationSection("suffixes"), player);
        addOwnedSuffixes(suffixes, customSuffixes, player);
        return suffixes;
    }

    private Map<String, List<String>> getRegisteredSuffixes() {
        Map<String, List<String>> suffixes = new LinkedHashMap<>();
        addRegisteredSuffixes(suffixes, getConfig().getConfigurationSection("suffixes"));
        addRegisteredSuffixes(suffixes, customSuffixes);
        return suffixes;
    }

    private void addOwnedSuffixes(List<String> suffixes, ConfigurationSection section, Player player) {
        if (section == null) {
            return;
        }
        for (String suffix : section.getKeys(false)) {
            if (isSuffixOwner(section.getStringList(suffix), player) && !containsSuffix(suffixes, suffix)) {
                suffixes.add(suffix);
            }
        }
    }

    private void addRegisteredSuffixes(Map<String, List<String>> suffixes, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String suffix : section.getKeys(false)) {
            suffixes.putIfAbsent(suffix, section.getStringList(suffix));
        }
    }

    private boolean containsSuffix(List<String> suffixes, String requestedSuffix) {
        String colored = color(requestedSuffix);
        for (String suffix : suffixes) {
            if (suffix.equalsIgnoreCase(requestedSuffix) || color(suffix).equalsIgnoreCase(colored)) {
                return true;
            }
        }
        return false;
    }

    private String findOwnedSuffix(Player player, String requestedSuffix) {
        String requestedColored = color(requestedSuffix);
        for (String suffix : getOwnedSuffixes(player)) {
            if (suffix.equalsIgnoreCase(requestedSuffix) || color(suffix).equalsIgnoreCase(requestedColored)) {
                return suffix;
            }
        }
        return null;
    }

    private boolean ownsSuffix(Player player, String suffix) {
        ConfigurationSection configuredSuffixes = getConfig().getConfigurationSection("suffixes");
        return (configuredSuffixes != null && isSuffixOwner(configuredSuffixes.getStringList(suffix), player))
                || (customSuffixes != null && isSuffixOwner(customSuffixes.getStringList(suffix), player));
    }

    private boolean isSuffixOwner(List<String> owners, Player player) {
        for (String owner : owners) {
            if (owner == null) {
                continue;
            }
            if (owner.equalsIgnoreCase(player.getName()) || owner.equalsIgnoreCase(player.getUniqueId().toString())) {
                return true;
            }
        }
        return false;
    }

    private void addSuffixOwner(String suffix, String playerName) {
        if (customSuffixes == null) {
            return;
        }

        List<String> owners = new ArrayList<>(customSuffixes.getStringList(suffix));
        boolean contains = false;
        for (String owner : owners) {
            if (owner.equalsIgnoreCase(playerName)) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            owners.add(playerName);
        }
        customSuffixes.set(suffix, owners);
    }

    private String normalizeSuffixKey(String input) {
        String suffix = input == null ? "" : input.trim().replace(ChatColor.COLOR_CHAR, '&');
        String configuredColorChar = getConfig().getString("settings.color-char", "^");
        char colorChar = configuredColorChar == null || configuredColorChar.isBlank() ? '^' : configuredColorChar.charAt(0);
        if (colorChar != '&') {
            suffix = convertColorCharacter(suffix, colorChar);
        }
        return suffix;
    }

    private String convertColorCharacter(String input, char colorChar) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (current == colorChar && index + 1 < input.length() && isColorCode(input.charAt(index + 1))) {
                builder.append('&').append(Character.toLowerCase(input.charAt(index + 1)));
                index++;
                continue;
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private boolean isColorCode(char value) {
        return (value >= '0' && value <= '9')
                || (value >= 'a' && value <= 'f')
                || (value >= 'A' && value <= 'F')
                || "klmnorKLMNOR".indexOf(value) >= 0;
    }

    private int getCustomCount(Player player) {
        return getCustomCount(player.getUniqueId(), player.getName());
    }

    private int getCustomCount(UUID uuid, String playerName) {
        if (permissionStorage == null) {
            return 0;
        }
        String nameKey = playerName == null || playerName.isBlank() ? uuid.toString() : playerName;
        int count = permissionStorage.getInt(nameKey, -1);
        if (count >= 0) {
            return count;
        }
        return Math.max(0, permissionStorage.getInt("players." + uuid + ".count", 0));
    }

    private void addCustomCount(UUID uuid, String playerName, int amount) {
        setCustomCount(uuid, playerName, getCustomCount(uuid, playerName) + amount);
    }

    private void setCustomCount(UUID uuid, String playerName, int count) {
        if (permissionStorage == null) {
            return;
        }
        String nameKey = playerName == null || playerName.isBlank() ? uuid.toString() : playerName;
        permissionStorage.set(nameKey, Math.max(0, count));
    }

    private boolean canCreateWithoutCount(Player player) {
        return player.hasPermission(BYPASS_PERMISSION) || player.hasPermission(ADMIN_PERMISSION);
    }

    private void ensureStorage() throws IOException {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IOException("Could not create plugin data folder");
        }
        permissionFile = ensureYamlResource(PERMISSION_FILE_NAME);
        suffixesFile = ensureYamlResource(SUFFIXES_FILE_NAME);
    }

    private void reloadStorage() {
        permissionFile = new File(getDataFolder(), PERMISSION_FILE_NAME);
        suffixesFile = new File(getDataFolder(), SUFFIXES_FILE_NAME);
        permissionStorage = YamlConfiguration.loadConfiguration(permissionFile);
        customSuffixes = YamlConfiguration.loadConfiguration(suffixesFile);
    }

    private void saveStorage() {
        saveYaml(permissionStorage, permissionFile, PERMISSION_FILE_NAME);
        saveYaml(customSuffixes, suffixesFile, SUFFIXES_FILE_NAME);
    }

    private File ensureYamlResource(String fileName) throws IOException {
        File file = new File(getDataFolder(), fileName);
        if (!file.isFile()) {
            try (InputStream resource = getResource(fileName)) {
                if (resource == null) {
                    if (!file.createNewFile()) {
                        throw new IOException("Could not create " + fileName);
                    }
                    return file;
                }
                saveResource(fileName, false);
            }
        }
        return file;
    }

    private void saveYaml(YamlConfiguration yaml, File file, String fileName) {
        if (yaml == null || file == null) {
            return;
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Could not save " + fileName, exception);
        }
    }

    private int inventorySize() {
        int configuredSize = getConfig().getInt("inventory.size", 6);
        int size = configuredSize <= 6 ? configuredSize * 9 : configuredSize;
        size = Math.max(9, Math.min(54, size));
        return size - (size % 9);
    }

    private List<Integer> configuredSuffixSlots() {
        List<Integer> slots = parseSlots(getConfig().get("inventory.suffix-slots"));
        if (slots.isEmpty()) {
            for (int slot = 0; slot < inventorySize(); slot++) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private List<Integer> parseSlots(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }

        List<Integer> slots = new ArrayList<>();
        if (rawValue instanceof Collection<?> collection) {
            for (Object value : collection) {
                addParsedSlots(slots, String.valueOf(value));
            }
        } else {
            addParsedSlots(slots, String.valueOf(rawValue));
        }
        return slots;
    }

    private void addParsedSlots(List<Integer> slots, String rawSlots) {
        for (String part : rawSlots.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                slots.add(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean isInventorySlot(int slot, int inventorySize) {
        return slot >= 0 && slot < inventorySize;
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private void dispatchConsoleCommand(String command) {
        if (command == null || command.isBlank()) {
            return;
        }

        String prepared = command.trim();
        while (prepared.startsWith("/")) {
            prepared = prepared.substring(1);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
    }

    private void sendCustomHelp(CommandSender sender) {
        send(sender, "&6&lКАСТОМИЗАЦИЯ: &f/suffixcustom set <титул>", Map.of());
        send(sender, "&6&lКАСТОМИЗАЦИЯ: &f/suffixcustom count", Map.of());
        if (hasAccess(sender, GIVE_PERMISSION)) {
            send(sender, "&6&lКАСТОМИЗАЦИЯ: &f/suffixcustom give <ник> <количество>", Map.of());
        }
    }

    private void sendUsage(CommandSender sender, String usage) {
        send(sender, getConfig().getString("messages.usage", "&6&lКАСТОМИЗАЦИЯ: &fИспользование &f&l➲ &6&l%usage%"), Map.of("usage", usage));
    }

    private void sendConfiguredMessage(CommandSender sender, String key, Map<String, String> variables) {
        String path = "messages." + key;
        if (getConfig().isList(path)) {
            for (String line : getConfig().getStringList(path)) {
                send(sender, line, variables);
            }
            return;
        }

        String message = getConfig().getString(path);
        if (message == null || message.isBlank()) {
            return;
        }
        for (String part : message.split("\\R", -1)) {
            send(sender, part, variables);
        }
    }

    private void send(CommandSender sender, String message, Map<String, String> variables) {
        String prepared = color(replaceVariables(message, variables));
        if (sender instanceof Player) {
            sender.sendMessage(prepared);
        } else {
            sender.sendMessage(ChatColor.stripColor(prepared));
        }
    }

    private String replaceVariables(String input, Map<String, String> variables) {
        String result = input == null ? "" : input;
        if (variables == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("%" + entry.getKey() + "%", value);
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    private String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input.replace(ChatColor.COLOR_CHAR, '&'));
    }

    private int parsePositiveInt(String input) {
        try {
            int parsed = Integer.parseInt(input);
            return parsed > 0 ? parsed : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private boolean hasAccess(CommandSender sender, String permission) {
        return !(sender instanceof Player player)
                || player.hasPermission(permission)
                || player.hasPermission(ADMIN_PERMISSION);
    }

    private List<String> completeSuffixCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("open", "set"));
            if (hasAccess(sender, GIVE_PERMISSION)) {
                options.add("give");
            }
            if (hasAccess(sender, RELOAD_PERMISSION)) {
                options.add("reload");
            }
            return complete(args[0], options);
        }
        if (args.length == 2 && List.of("give").contains(args[0].toLowerCase(Locale.ROOT))) {
            return complete(args[1], onlinePlayerNames());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender instanceof Player player) {
            return complete(args[1], getOwnedSuffixes(player));
        }
        return Collections.emptyList();
    }

    private List<String> completeSuffixCustomCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("set", "count", "cancel"));
            if (hasAccess(sender, GIVE_PERMISSION)) {
                options.add("give");
            }
            if (hasAccess(sender, RELOAD_PERMISSION)) {
                options.add("reload");
            }
            return complete(args[0], options);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("count"))) {
            return complete(args[1], onlinePlayerNames());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return complete(args[2], List.of("1", "3", "5", "10"));
        }
        return Collections.emptyList();
    }

    private List<String> complete(String prefix, Collection<String> variants) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String variant : variants) {
            if (variant.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(variant);
            }
        }
        return result;
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private static Method findLegacyMatchMaterialMethod() {
        try {
            return Material.class.getDeclaredMethod("matchMaterial", String.class, boolean.class);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private enum CreateResult {
        DONE,
        INVALID
    }

    private interface GuiAction {
    }

    private static final class SuffixAction implements GuiAction {
        private final String suffix;

        private SuffixAction(String suffix) {
            this.suffix = suffix;
        }
    }

    private static final class StaticItemAction implements GuiAction {
        private final boolean backItem;
        private final boolean nextItem;
        private final boolean createTitle;
        private final boolean close;
        private final List<String> commands;
        private final List<String> commandsIfFirstPage;
        private final List<String> messages;

        private StaticItemAction(
                boolean backItem,
                boolean nextItem,
                boolean createTitle,
                boolean close,
                List<String> commands,
                List<String> commandsIfFirstPage,
                List<String> messages
        ) {
            this.backItem = backItem;
            this.nextItem = nextItem;
            this.createTitle = createTitle;
            this.close = close;
            this.commands = commands;
            this.commandsIfFirstPage = commandsIfFirstPage;
            this.messages = messages;
        }

        private static StaticItemAction from(ConfigurationSection section) {
            return new StaticItemAction(
                    section.getBoolean("back-item", false),
                    section.getBoolean("next-item", false),
                    section.getBoolean("create-title", false),
                    section.getBoolean("close", false),
                    section.getStringList("commands"),
                    section.getStringList("commands-if-first-page"),
                    section.getStringList("messages")
            );
        }
    }

    private static final class SuffixInventoryHolder implements InventoryHolder {
        private final int page;
        private final int maxPage;
        private final Map<Integer, GuiAction> actions = new HashMap<>();
        private Inventory inventory;

        private SuffixInventoryHolder(int page, int maxPage) {
            this.page = page;
            this.maxPage = maxPage;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
