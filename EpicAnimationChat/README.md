# EpicAnimationChat

EpicAnimationChat reads animations directly from `plugins/TAB/animations.yml`
and replaces `%animation:name%` placeholders in Paper chat components.

It wraps the chat renderer that is already installed, so prefixes and formats
from another chat plugin are preserved.

Example TAB animation:

```yaml
moder:
  change-interval: 200
  texts:
    - "&cModerator"
    - "&6Moderator"
    - "&eModerator"
```

Use the placeholder in the chat format:

```text
%animation:moder% %player_name%: %message%
```

Commands:

- `/eac reload`
- `/eac list`
- `/eac preview <animation>`

Minecraft chat lines cannot be edited after they have been sent. The plugin
therefore inserts the animation frame that is current when each message is
rendered; old messages remain unchanged.
