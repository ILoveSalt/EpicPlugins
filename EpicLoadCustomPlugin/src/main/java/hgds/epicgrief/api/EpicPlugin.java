package hgds.epicgrief.api;

public interface EpicPlugin {

    default void onEpicPluginConnected(EpicPluginContext context) {
    }

    default void onEpicPluginDisconnected(EpicPluginContext context) {
    }
}
