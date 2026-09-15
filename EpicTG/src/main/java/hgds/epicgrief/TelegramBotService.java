package hgds.epicgrief;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TelegramBotService {
    interface MessageHandler {
        void handle(long chatId, long telegramId, String username, String firstName, String text);
    }

    private final JavaPlugin plugin;
    private final String token;
    private final int timeoutSeconds;
    private final MessageHandler messageHandler;
    private final HttpClient httpClient;
    private volatile boolean running;
    private Thread pollingThread;
    private long offset;

    TelegramBotService(JavaPlugin plugin, String token, int timeoutSeconds, MessageHandler messageHandler) {
        this.plugin = plugin;
        this.token = token;
        this.timeoutSeconds = timeoutSeconds;
        this.messageHandler = messageHandler;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    void start() {
        if (running) {
            return;
        }

        running = true;
        pollingThread = new Thread(this::pollLoop, "EpicTG-TelegramPolling");
        pollingThread.setDaemon(true);
        pollingThread.start();
        plugin.getLogger().info("Telegram bot polling started.");
    }

    void stop() {
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
            pollingThread = null;
        }
    }

    void sendMessage(long chatId, String text, Object replyMarkup) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }

        HttpRequest request = buildPostRequest("sendMessage", body, Duration.ofSeconds(15));
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("Telegram sendMessage failed: " + throwable.getMessage());
                        return;
                    }
                    if (response.statusCode() >= 400) {
                        plugin.getLogger().warning("Telegram sendMessage returned HTTP " + response.statusCode() + ": " + response.body());
                    }
                });
    }

    static Map<String, Object> replyKeyboard(List<List<String>> rows) {
        List<Object> keyboard = new ArrayList<>();
        for (List<String> row : rows) {
            List<Object> keyboardRow = new ArrayList<>();
            for (String button : row) {
                Map<String, Object> buttonObject = new LinkedHashMap<>();
                buttonObject.put("text", button);
                keyboardRow.add(buttonObject);
            }
            keyboard.add(keyboardRow);
        }

        Map<String, Object> markup = new LinkedHashMap<>();
        markup.put("keyboard", keyboard);
        markup.put("resize_keyboard", true);
        markup.put("one_time_keyboard", false);
        return markup;
    }

    private void pollLoop() {
        while (running) {
            try {
                pollOnce();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                plugin.getLogger().warning("Telegram polling error: " + exception.getMessage());
                sleepAfterError();
            }
        }
        plugin.getLogger().info("Telegram bot polling stopped.");
    }

    private void pollOnce() throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timeout", timeoutSeconds);
        body.put("offset", offset);
        body.put("allowed_updates", Arrays.asList("message"));

        HttpRequest request = buildPostRequest("getUpdates", body, Duration.ofSeconds(timeoutSeconds + 10L));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("getUpdates returned HTTP " + response.statusCode() + ": " + response.body());
        }

        Object parsed = SimpleJson.parse(response.body());
        Map<String, Object> root = asObject(parsed);
        if (root == null || !Boolean.TRUE.equals(root.get("ok"))) {
            throw new IOException("Telegram getUpdates response is not ok: " + response.body());
        }

        List<Object> updates = asArray(root.get("result"));
        if (updates == null) {
            return;
        }

        for (Object updateValue : updates) {
            Map<String, Object> update = asObject(updateValue);
            if (update == null) {
                continue;
            }

            long updateId = asLong(update.get("update_id"));
            offset = Math.max(offset, updateId + 1L);
            handleUpdate(update);
        }
    }

    private void handleUpdate(Map<String, Object> update) {
        Map<String, Object> message = asObject(update.get("message"));
        if (message == null) {
            return;
        }

        String text = asString(message.get("text"));
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Map<String, Object> chat = asObject(message.get("chat"));
        Map<String, Object> from = asObject(message.get("from"));
        if (chat == null || from == null) {
            return;
        }

        long chatId = asLong(chat.get("id"));
        long telegramId = asLong(from.get("id"));
        String username = asString(from.get("username"));
        String firstName = asString(from.get("first_name"));

        if (!plugin.isEnabled()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () ->
                messageHandler.handle(chatId, telegramId, username, firstName, text));
    }

    private HttpRequest buildPostRequest(String method, Map<String, Object> body, Duration timeout) {
        return HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + token + "/" + method))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(SimpleJson.stringify(body), StandardCharsets.UTF_8))
                .build();
    }

    private void sleepAfterError() {
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asObject(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> asArray(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return null;
    }

    private String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
