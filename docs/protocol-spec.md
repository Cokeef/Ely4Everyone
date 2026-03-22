# Ely4Everyone Protocol Spec v1

Этот документ фиксирует минимальный протокол для первой рабочей версии.

## Цели

- дать игроку с Fabric-модом долгоживущую клиентскую `auth session`, а на логине выпускать short-lived Ely ticket под конкретный challenge;
- позволить Velocity plugin проверить ticket до передачи игрока на backend;
- сохранить fallback на cracked/AuthMe flow, если Ely ticket отсутствует или невалиден;
- не передавать в клиент `client_secret`.

## Компоненты

- `mod` - клиентский Fabric-мод, который хранит локальный `auth_session_token`, получает login ticket по challenge и отвечает на login challenge;
- `velocity-plugin` - proxy plugin, который валидирует ticket, поднимает встроенный auth-host и выбирает trusted Ely mode или fallback.
- `paper-bridge` - backend plugin для `Paper`, который авто-логинит доверенных игроков в `AuthMe`-подобном окружении.

## Канал логина

- Channel ID: `ely4everyone:login`
- Транспорт: Velocity login plugin message

Плагин на proxy инициирует login challenge на стадии логина. Клиент с модом либо получает login ticket через встроенный auth-host и отправляет его, либо отвечает пустым payload.

## Login Flow

### 1. Предварительный Ely login

До подключения к серверу мод должен получить клиентскую Ely auth session через встроенный auth-host:

1. игрок нажимает "Войти через Ely.by";
2. мод открывает браузер;
3. встроенный auth-host в Velocity завершает OAuth code flow на своей стороне;
4. plugin выдает `auth_session_token`;
5. мод хранит `auth_session_token` локально до истечения TTL.

### 2. Server join

При подключении к серверу:

1. proxy отправляет challenge через `ely4everyone:login`;
2. мод по `session_token + nonce + audience` запрашивает у auth-host login ticket;
3. мод отвечает одним из двух вариантов:
   - `ticket=<signed ticket>`
   - `ticket=` если Ely session отсутствует или ticket не удалось получить;
4. proxy валидирует подпись, issuer, expiry, nonce и replay protection;
5. если ticket валиден, игрок помечается как trusted Ely user;
6. если ticket невалиден или отсутствует, игрок переводится в cracked/AuthMe flow.

## Embedded Auth HTTP API

### Реализовано сейчас

- `GET /health`
- `GET /api/v1/config`
- `GET /api/v1/auth/start`
- `GET /api/v1/auth/poll`
- `GET /api/v1/auth/issue-ticket`
- `POST /api/v1/dev/tickets`
- `GET /oauth/callback`

### Уже реализовано в MVP

- получение Ely textures properties через Ely session profile
- выдача challenge-bound login ticket
- persistence клиентских auth sessions на стороне proxy
- backend Paper bridge для auto-login команды

### Следующие улучшения

- refresh flow через `offline_access`
- более строгий trust contract между proxy и backend, если понадобится отличать Ely4Everyone от обычного premium forwarding

## Формат Ticket

Текущая черновая форма:

- строка вида `<payload>.<signature>`;
- `payload` - base64url от текстового набора claim-строк;
- `signature` - HMAC-SHA256 от `payload`.

### Claims

- `ver` - версия формата, сейчас `v1`
- `iss` - issuer auth-host
- `aud` - аудитория, обычно id сети или proxy
- `sub` - Ely UUID игрока
- `name` - Ely username
- `iat` - время выпуска в epoch seconds
- `exp` - время истечения в epoch seconds
- `jti` - уникальный ticket id
- `nonce` - nonce login challenge, который должен совпасть на proxy

## Правила валидации на proxy

- подпись должна совпадать с ключом доверенного auth-host;
- `iss` должен находиться в allowlist;
- `exp` должен быть в будущем;
- `iat` не должен быть слишком далеко в будущем;
- `aud` должен совпадать с id сети, если аудитория включена;
- `jti` не должен приниматься повторно в окне TTL.
- `nonce` должен совпадать с тем challenge, который proxy отправил конкретному клиенту.

## Правила fallback

Если любое из условий ниже не выполнено, Ely trusted mode не включается:

- клиент не ответил на challenge;
- клиент ответил пустым ticket;
- подпись не сошлась;
- ticket истек;
- issuer не доверен;
- replay detection сработал;
- auth-host временно недоступен и мод не может выпустить ticket из своей auth session.

В этом случае игрок остается обычным cracked-user и идет по `AuthMe` или иному fallback сценарию.

## Dev Flow

Помимо OAuth flow, встроенный auth-host умеет выпускать dev tickets через `POST /api/v1/dev/tickets`.

Этот endpoint нужен для локальной разработки протокола и проверки связки auth-host -> proxy verifier.
