# [huemoe](https://github.com/dixel/huemoe)
💡 Telegram bot to control your Philips Hue -based smart devices 💡

[![CircleCI](https://circleci.com/gh/dixel/huemoe.svg?style=svg)](https://circleci.com/gh/dixel/huemoe)
[![Built with Spacemacs](https://cdn.rawgit.com/syl20bnr/spacemacs/442d025779da2f62fc86c2082703697714db6514/assets/spacemacs-badge.svg)](http://spacemacs.org)

![](./pic/screenshot.png)

## Usage

### Prerequisites
- Your Philips Hue system IP address;
- Developer token for your Hue system (can be obtained this way [link](https://developers.meethue.com/documentation/getting-started));
- Telegram API token (you would need to register a bot [link](https://core.telegram.org/bots#6-botfather);
- List of users to allow using this bot (Telegram user unique identifiers (those that start with @ usually));

```
docker run -d \
    -e HUE_HOST=<IP address of your Hue bridge> \
    -e HUE_TOKEN=<Hue developer token> \
    -e TELEGRAM_USER_WHITELIST=<Comma-separated list of allowed telegram users> \
    -e TELEGRAM_TOKEN=<Telegram developer token>
    dixel/huemoe:0.0.4.3
```

## Motivation
There are few telegram bots implemented in clojure, even though the development process
is full of joy:
- Controlling your lamp from REPl
- Asking your homemates to play with the bot and the lamp
- Having awesome clojure data structures and libraries for processing the state of the bot and the lights


### Try it with REPl

```clojure
(mount/start)
```

## License

Copyright © 2017 Avdiushkin Vasilii

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
