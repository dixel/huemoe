# huemoe
💡 Telegram bot to control your Philips Hue -based smart devices 💡

![](./pic/screenshot.png)

## Motivation
There are few telegram bots implemented with clojure, even though the development process
is full of joy:
- Controlling your lamp from REPl
- Asking your homemates to play with the bot and the lamp
- Having awesome clojure data structures and libraries for processing the state of the bot and the lights

## Usage

### Prerequisites
- Your Philips Hue system IP address;
- Developer token for your Hue system (can be obtained this way [link](https://developers.meethue.com/documentation/getting-started));
- Telegram API token (you would need to register a bot [link](https://core.telegram.org/bots#6-botfather);
- List of users to allow using this bot (Telegram user unique identifiers (those that start with @ usually));
- `.config.edn` having the same flavour as `example.config.edn` containing the preceding information.

### Try it with REPl

```clojure
(mount/start)
```

### Use your dusty RPi/nettop
```
lein uberjar
rsync target/huemoe-0.1.0-SNAPSHOT-standalone.jar
```

[![Built with Spacemacs](https://cdn.rawgit.com/syl20bnr/spacemacs/442d025779da2f62fc86c2082703697714db6514/assets/spacemacs-badge.svg)](http://spacemacs.org)

## License

Copyright © 2017 Avdiushkin Vasilii

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
