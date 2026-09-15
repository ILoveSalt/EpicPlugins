package hgds.epicAntiRelog.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ParsedCommand {

    private final String label;
    private final List<String> arguments;

    private ParsedCommand(String label, List<String> arguments) {
        this.label = label;
        this.arguments = arguments;
    }

    public static ParsedCommand from(String rawCommand) {
        String normalized = rawCommand == null ? "" : rawCommand.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.isBlank()) {
            return new ParsedCommand("", List.of());
        }

        String[] parts = normalized.split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        List<String> arguments = new ArrayList<>();

        for (int index = 1; index < parts.length; index++) {
            arguments.add(parts[index]);
        }

        return new ParsedCommand(label, List.copyOf(arguments));
    }

    public String label() {
        return label;
    }

    public List<String> arguments() {
        return arguments;
    }

    public boolean hasArgument(String argument) {
        for (String currentArgument : arguments) {
            if (currentArgument.equalsIgnoreCase(argument)) {
                return true;
            }
        }

        return false;
    }
}
