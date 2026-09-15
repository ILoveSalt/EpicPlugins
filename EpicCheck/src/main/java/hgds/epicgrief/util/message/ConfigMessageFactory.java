package hgds.epicgrief.util.message;

import hgds.epicgrief.util.StringUtil;
import org.bukkit.configuration.ConfigurationSection;

public class ConfigMessageFactory {
    private final String prefix;

    public ConfigMessageFactory(String prefix) {
        this.prefix = (prefix == null) ? null : StringUtil.colorize(prefix);
    }

    public Message create(Object object) {
        if (object instanceof String)
            return create((String)object);
        if (object instanceof ConfigurationSection)
            return create((ConfigurationSection)object);
        throw new IllegalArgumentException("Unsupported type");
    }

    public Message create(String content) {
        return new Message(formatChat(content), null, null);
    }

    public Message create(ConfigurationSection section) {
        String chatContent = null;
        String titleContent = null;
        String actionbarContent = null;
        if (section.isString("chat"))
            chatContent = formatChat(section.getString("chat"));
        if (section.isString("title"))
            titleContent = section.getString("title");
        if (section.isString("actionbar"))
            actionbarContent = section.getString("actionbar");
        return new Message(chatContent, titleContent, actionbarContent);
    }

    private String formatChat(String content) {
        return (this.prefix == null) ? content : (this.prefix + content);
    }
}