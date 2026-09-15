package hgds.epicgrief;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class AnimationChatListener implements Listener {
    private final EpicAnimationChat plugin;
    private final TabAnimationService animationService;

    public AnimationChatListener(EpicAnimationChat plugin, TabAnimationService animationService) {
        this.plugin = plugin;
        this.animationService = animationService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) {
            return;
        }

        ChatRenderer current = event.renderer();
        if (current instanceof AnimationRenderer) {
            return;
        }
        event.renderer(new AnimationRenderer(current, animationService));
    }

    private record AnimationRenderer(
            ChatRenderer delegate,
            TabAnimationService animationService
    ) implements ChatRenderer {
        @Override
        public Component render(
                Player source,
                Component sourceDisplayName,
                Component message,
                net.kyori.adventure.audience.Audience viewer
        ) {
            Component rendered = delegate.render(source, sourceDisplayName, message, viewer);
            return animationService.replaceAnimations(rendered, source);
        }
    }
}
