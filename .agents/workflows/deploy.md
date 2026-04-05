---
description: Собрать мод и задеплоить JAR на proxy-сервер для загрузки на ПК
---

# Deploy: Сборка и деплой мода

Этот workflow собирает мод и доставляет JAR прямо на ПК пользователя через Tailscale.

## Шаги

// turbo-all

1. Запустить скрипт деплоя:
```bash
bash /opt/Ely4Everyone/deploy.sh
```

## Как это работает
- Креды берутся из `/opt/.env` (SCP_USER, SCP_PASS, SCP_TARGET)
- Мод собирается через `./gradlew :mod:build`
- JAR копируется на ПК через sshpass + scp по Tailscale (100.104.97.76)
- Из-за пробелов в пути Windows, JAR сначала попадает в `D:/`, потом перемещается в папку модов
- Финальный путь: `D:\Games\AstralRinth App\profiles\Ely4Everyone\mods\`
