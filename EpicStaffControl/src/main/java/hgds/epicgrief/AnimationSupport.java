package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

public final class AnimationSupport {
    private final EpicStaffControl plugin;

    private Plugin provider;
    private Method replaceMethod;
    private boolean warningLogged;

    public AnimationSupport(EpicStaffControl plugin) {
        this.plugin = plugin;
    }

    public Component replace(Component component, Player source) {
        Plugin currentProvider = plugin.getServer().getPluginManager().getPlugin("EpicAnimationChat");
        if (currentProvider == null || !currentProvider.isEnabled()) {
            clearProvider();
            return component;
        }

        try {
            if (currentProvider != provider || replaceMethod == null) {
                provider = currentProvider;
                replaceMethod = provider.getClass().getMethod(
                        "replaceAnimations",
                        Component.class,
                        Player.class
                );
                warningLogged = false;
            }

            Object result = replaceMethod.invoke(provider, component, source);
            return result instanceof Component resolved ? resolved : component;
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!warningLogged) {
                warningLogged = true;
                plugin.getLogger().log(
                        Level.WARNING,
                        "EpicAnimationChat is installed, but its animation API is unavailable.",
                        exception
                );
            }
            return component;
        }
    }

    private void clearProvider() {
        provider = null;
        replaceMethod = null;
        warningLogged = false;
    }
}
