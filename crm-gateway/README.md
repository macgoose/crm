# crm-gateway

Внешняя точка входа в CRM. Сервис аутентифицирует запросы по JWT Avanpost FAM, сопоставляет HTTP-запрос с правилом маршрутизации, получает RBAC-решение от `crm-user-service` и проксирует только разрешённые запросы.

## Место в системе

```text
Клиент
  -> JWT authentication
crm-gateway
  -> login/email [+ permission для PERMISSION route]
crm-user-service
  -> allowed + userId + userVersion
crm-gateway
  -> trusted identity headers
Целевой CRM-сервис
```

Gateway реализован на Spring Boot 4, Spring Security Resource Server и Spring Cloud Gateway Server MVC. Правила маршрутизации хранятся в собственной БД и создаются Liquibase.

## Обработка запроса

1. Spring Security проверяет подпись JWT, `iss` и `aud`.
2. `AvanpostIdentityResolver` получает login и email из JWT или `/oauth2/userinfo`, в зависимости от `IDENTITY_SOURCE`.
3. `RouteRuleMatcher` выбирает первое подходящее включённое правило по `rule_order`.
4. Для `AUTHENTICATED` Gateway разрешает активного пользователя по login/email; для `PERMISSION` дополнительно запрашивает RBAC-решение по `permission_code`.
5. При положительном решении входящие trusted headers заменяются значениями `userId` и `userVersion` из ответа.
6. Запрос проксируется на `target_uri`. Если URI содержит путь, исходный путь заменяется; query parameters сохраняются.

Неизвестные маршруты и отрицательные решения запрещаются по принципу deny by default.

## Локальный запуск

Требуются JDK 21+ и Maven. Из корня репозитория:

```powershell
mvn -pl crm-gateway -am install
mvn -pl crm-gateway spring-boot:run
```

Перед Gateway необходимо запустить `crm-user-service`. По умолчанию:

- Gateway: `http://localhost:8080`;
- User Service: `http://localhost:8081`;
- целевой сервис dev-маршрута: `http://localhost:8082`.

Dev-контекст Liquibase добавляет маршрут:

```text
GET /api/v1/organizations/**
permission: ORGANIZATION_READ
target: http://localhost:8082
order: 100
```

и legacy-маршрут без проверки permission:

```text
POST /crm/terminal-api/v1/CrmServiceRest
access mode: AUTHENTICATED
target: http://192.168.151.14:8080/crm/terminal-api/v1/CrmServiceRest
order: 200
```

Для запуска без демонстрационного маршрута задайте `LIQUIBASE_CONTEXTS=prod`.

## Конфигурация

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP-порт Gateway |
| `AVANPOST_ISSUER_URI` | `https://mfa.spimex.com` | ожидаемый `iss` JWT |
| `AVANPOST_JWK_SET_URI` | `https://mfa.spimex.com/oauth2/jwks` | JWKS для проверки подписи |
| `AVANPOST_AUDIENCE` | `crm` | ожидаемый `aud` |
| `AVANPOST_USERINFO_URI` | `https://mfa.spimex.com/oauth2/userinfo` | endpoint UserInfo |
| `IDENTITY_SOURCE` | `JWT` | `JWT`, `USERINFO` или `JWT_WITH_USERINFO_FALLBACK` |
| `IDENTITY_LOGIN_CLAIM` | `preferred_username` | claim с login |
| `IDENTITY_EMAIL_CLAIM` | `email` | claim с email |
| `IDENTITY_EMAIL_VERIFIED_CLAIM` | `email_verified` | claim подтверждения email |
| `IDENTITY_TRUST_UNVERIFIED_EMAIL` | `false` | разрешить неподтверждённый email |
| `CRM_USER_SERVICE_URL` | `http://localhost:8081` | адрес User Service |
| `CRM_USER_SERVICE_CONNECT_TIMEOUT` | `2s` | таймаут соединения с User Service |
| `CRM_USER_SERVICE_READ_TIMEOUT` | `3s` | таймаут ответа User Service |
| `DB_URL` | `jdbc:h2:file:./crm-gateway;AUTO_SERVER=TRUE` | JDBC URL |
| `DB_USERNAME` | `crm` | пользователь БД |
| `DB_PASSWORD` | `crm` | пароль БД |
| `LIQUIBASE_CONTEXTS` | `dev` | активные контексты Liquibase |

Пример PostgreSQL:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/crm_gateway"
$env:DB_USERNAME = "crm"
$env:DB_PASSWORD = "secret"
```

Файловая H2 использует `AUTO_SERVER=TRUE`, поэтому к ней можно подключаться внешним клиентом во время работы приложения. Относительный путь вычисляется от рабочей директории процесса.

## Правила маршрутизации

Таблица `gateway_route_rule` содержит:

| Поле | Назначение |
|---|---|
| `route_key` | уникальное имя маршрута |
| `http_method` | конкретный HTTP-метод; wildcard не поддерживается |
| `path_pattern` | Spring path pattern |
| `access_mode` | `AUTHENTICATED` или `PERMISSION` |
| `permission_code` | обязательный permission для `PERMISSION`; `NULL` для `AUTHENTICATED` |
| `target_uri` | адрес целевого сервиса |
| `enabled` | признак активности |
| `rule_order` | порядок проверки; первое совпадение побеждает |

Правила компилируются и проверяются при старте. Некорректный метод, pattern, URI, сочетание режима и permission либо дублирующая пара method/path останавливают запуск. Административного API и hot reload пока нет, поэтому изменения БД начинают действовать после перезапуска.

## Ответы безопасности

| HTTP | Ситуация |
|---|---|
| `401` | JWT отсутствует или не прошёл аутентификацию |
| `403` | маршрут неизвестен, identity некорректна или доступ запрещён |
| `503` | недоступен identity provider или `crm-user-service` |

`/actuator/health` доступен без аутентификации. Остальные запросы требуют JWT.

## Структура кода

- `api` — authorization filter и обёртка запроса с trusted headers;
- `application` — компиляция и поиск route rules;
- `domain` — JPA-модель и репозиторий правил;
- `routing` — построение proxy routes;
- `security` — JWT-конфигурация и получение внешней identity;
- `user` — настройка клиента `crm-user-service`.

## Проверка

Из корня репозитория:

```powershell
mvn -pl crm-gateway -am test
```

При проверке миграций удалите только тестовую/локальную БД нужного сервиса и запустите приложение на чистой схеме.
