package hgds.epicgrief;

import com.google.common.collect.ImmutableList;
import hgds.epicgrief.util.TimeUtil;
import hgds.epicgrief.util.command.ExecutableCommand;
import hgds.epicgrief.util.message.ConfigMessageFactory;
import hgds.epicgrief.util.message.Message;
import org.bukkit.configuration.ConfigurationSection;

public final class CheckConfig {
    private final Messages messages;

    private final CheckRestrictions restrictions;

    private final boolean playerInvulnerableWhileChecking;

    private final boolean enableIAmCheaterCmd;

    private final PunishCommands commands;

    public Messages getMessages() {
        return this.messages;
    }

    public CheckRestrictions getRestrictions() {
        return this.restrictions;
    }

    public PunishCommands getCommands() {
        return this.commands;
    }

    public CheckConfig(ConfigurationSection section) {
        this.messages = new Messages(section.getConfigurationSection("messages"));
        this.restrictions = new CheckRestrictions(section.getConfigurationSection("restrictions"));
        this.playerInvulnerableWhileChecking = section.getBoolean("player-invulnerable-while-checking");
        this.enableIAmCheaterCmd = section.getBoolean("features.iamcheater-cmd");
        this.commands = new PunishCommands(section.getConfigurationSection("commands"));
    }

    public boolean shouldPlayerBeInvulnerableWhileChecking() {
        return this.playerInvulnerableWhileChecking;
    }

    public boolean isIAmCheaterCmdEnabled() {
        return this.enableIAmCheaterCmd;
    }

    public static final class Messages {
        private final Message checkNotificationStart;

        private final Message checkNotificationRepeat;

        private final long checkNotificationPeriod;

        private final Message notPermitted;

        private final Message playerNotFound;

        private final Message checkCommandHelp;

        private final Message noSuchModeratorAction;

        private final Message playerIsNotBeingChecked;

        private final Message cannotCheckModerator;

        private final Message playerAlreadyBeingChecked;

        private final Message youAlreadyCheckingThisPlayer;

        private final Message youCannotCheckYourself;

        private final Message checkingPlayer;

        private final Message youPassedCheck;

        private final Message playerPassedCheck;

        private final Message playerFailedCheck;

        public Message getCheckNotificationStart() {
            return this.checkNotificationStart;
        }

        public Message getCheckNotificationRepeat() {
            return this.checkNotificationRepeat;
        }

        public long getCheckNotificationPeriod() {
            return this.checkNotificationPeriod;
        }

        public Message getNotPermitted() {
            return this.notPermitted;
        }

        public Message getPlayerNotFound() {
            return this.playerNotFound;
        }

        public Message getCheckCommandHelp() {
            return this.checkCommandHelp;
        }

        public Message getNoSuchModeratorAction() {
            return this.noSuchModeratorAction;
        }

        public Message getPlayerIsNotBeingChecked() {
            return this.playerIsNotBeingChecked;
        }

        public Message getCannotCheckModerator() {
            return this.cannotCheckModerator;
        }

        public Message getPlayerAlreadyBeingChecked() {
            return this.playerAlreadyBeingChecked;
        }

        public Message getYouAlreadyCheckingThisPlayer() {
            return this.youAlreadyCheckingThisPlayer;
        }

        public Message getYouCannotCheckYourself() {
            return this.youCannotCheckYourself;
        }

        public Message getCheckingPlayer() {
            return this.checkingPlayer;
        }

        public Message getYouPassedCheck() {
            return this.youPassedCheck;
        }

        public Message getPlayerPassedCheck() {
            return this.playerPassedCheck;
        }

        public Message getPlayerFailedCheck() {
            return this.playerFailedCheck;
        }

        public Messages(ConfigurationSection section) {
            String prefix = section.getString("chat-prefix");
            ConfigMessageFactory factory = new ConfigMessageFactory(prefix);
            ConfigurationSection checkNotificationSection = section.getConfigurationSection("check-notification");
            this.checkNotificationStart = factory.create(checkNotificationSection.get("start"));
            this.checkNotificationRepeat = factory.create(checkNotificationSection.get("repeat"));
            long checkNotificationPeriod = TimeUtil.fractionalSecondsToTicks(checkNotificationSection.getDouble("period"));
            this.checkNotificationPeriod = (checkNotificationPeriod == 0L) ? 1L : checkNotificationPeriod;
            this.notPermitted = factory.create(section.get("not-permitted"));
            this.playerNotFound = factory.create(section.get("player-not-found"));
            this.checkCommandHelp = factory.create(section.get("check-command-help"));
            this.noSuchModeratorAction = factory.create(section.get("no-such-moderator-action"));
            this.playerIsNotBeingChecked = factory.create(section.get("player-is-not-being-checked"));
            this.cannotCheckModerator = factory.create(section.get("cannot-check-moderator"));
            this.playerAlreadyBeingChecked = factory.create(section.get("player-already-being-checked"));
            this.youAlreadyCheckingThisPlayer = factory.create(section.get("you-already-checking-this-player"));
            this.youCannotCheckYourself = factory.create(section.get("you-cannot-check-yourself"));
            this.checkingPlayer = factory.create(section.get("checking-player"));
            this.youPassedCheck = factory.create(section.get("you-passed-check"));
            this.playerPassedCheck = factory.create(section.get("player-passed-check"));
            this.playerFailedCheck = factory.create(section.get("player-failed-check"));
        }
    }

    public static final class CheckRestrictions {
        private final boolean restrictMovement;

        private final boolean restrictChat;

        private final boolean restrictCommands;

        private final boolean restrictBlockPlacing;

        private final boolean restrictBlockBreaking;

        private final boolean restrictDroppingItems;

        private final boolean restrictInventoryInteraction;

        private final boolean restrictAttacking;

        private final ImmutableList<String> allowedCommands;

        public ImmutableList<String> getAllowedCommands() {
            return this.allowedCommands;
        }

        public CheckRestrictions(ConfigurationSection section) {
            this.restrictMovement = section.getBoolean("movement");
            this.restrictChat = section.getBoolean("chat");
            this.restrictCommands = section.getBoolean("commands.enable");
            this.restrictBlockPlacing = section.getBoolean("block-placing");
            this.restrictBlockBreaking = section.getBoolean("block-breaking");
            this.restrictDroppingItems = section.getBoolean("dropping-items");
            this.restrictInventoryInteraction = section.getBoolean("inventory-interaction");
            this.restrictAttacking = section.getBoolean("attacking");
            this

                    .allowedCommands = (ImmutableList<String>)section.getStringList("commands.whitelist").stream().filter(command -> !command.equals("iamcheater")).collect(ImmutableList.toImmutableList());
        }

        public boolean shouldRestrictMovement() {
            return this.restrictMovement;
        }

        public boolean shouldRestrictChat() {
            return this.restrictChat;
        }

        public boolean shouldRestrictCommands() {
            return this.restrictCommands;
        }

        public boolean shouldRestrictBlockPlacing() {
            return this.restrictBlockPlacing;
        }

        public boolean shouldRestrictBlockBreaking() {
            return this.restrictBlockBreaking;
        }

        public boolean shouldRestrictDroppingItems() {
            return this.restrictDroppingItems;
        }

        public boolean shouldRestrictInventoryInteraction() {
            return this.restrictInventoryInteraction;
        }

        public boolean shouldRestrictAttacking() {
            return this.restrictAttacking;
        }
    }

    public static final class PunishCommands {
        private final Executor executor;

        private final ExecutableCommand punishQuit;

        private final ExecutableCommand punishCheats;

        private final ExecutableCommand punishDodged;

        private final ExecutableCommand punishCheatFilesFound;

        private final ExecutableCommand punishIgnored;

        private final ExecutableCommand punishFair;

        public Executor getExecutor() {
            return this.executor;
        }

        public ExecutableCommand getPunishQuit() {
            return this.punishQuit;
        }

        public ExecutableCommand getPunishCheats() {
            return this.punishCheats;
        }

        public ExecutableCommand getPunishDodged() {
            return this.punishDodged;
        }

        public ExecutableCommand getPunishCheatFilesFound() {
            return this.punishCheatFilesFound;
        }

        public ExecutableCommand getPunishIgnored() {
            return this.punishIgnored;
        }

        public ExecutableCommand getPunishFair() {
            return this.punishFair;
        }

        public PunishCommands(ConfigurationSection section) {
            String executor = section.getString("executor");
            switch (executor) {
                case "console":
                    this.executor = Executor.CONSOLE;
                    break;
                case "moderator":
                    this.executor = Executor.MODERATOR;
                    break;
                default:
                    throw new RuntimeException("Wrong command executor");
            }
            this.punishQuit = new ExecutableCommand(section.getString("punish-quit"));
            this.punishCheats = new ExecutableCommand(section.getString("punish-cheats"));
            this.punishDodged = new ExecutableCommand(section.getString("punish-dodged"));
            this.punishCheatFilesFound = new ExecutableCommand(section.getString("punish-cheat-files-found"));
            this.punishIgnored = new ExecutableCommand(section.getString("punish-ignored"));
            this.punishFair = new ExecutableCommand(section.getString("punish-fair"));
        }

        public enum Executor {
            CONSOLE, MODERATOR;
        }
    }
}
