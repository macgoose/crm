# crm-user-service

Внутренний сервис пользователей и RBAC-авторизации CRM. Он сопоставляет подтверждённые Gateway атрибуты с внутренним пользователем и отвечает, имеет ли этот пользователь требуемое permission.

## Ответственность сервиса

- хранение пользователей, ролей и permissions;
- разрешение пользователя по login и/или email;
- отклонение неизвестных, неактивных и конфликтующих identities;
- проверка цепочки `User -> Role -> Permission`;
- возврат стабильного UUID пользователя и версии identity;
- хранение иерархических групп пользователей как административных метаданных.

Группы пользователей не участвуют в выдаче доступа. Источником authorization decision является только RBAC.

## Архитектура

Сервис использует слоистую структуру:

- `api` — внутренний HTTP-контракт, DTO и обработка ошибок;
- `application` — сценарий авторизации и причины отказа;
- `domain/model` — JPA-модели пользователей, ролей, permissions и групп;
- `domain/repository` — Spring Data repositories и RBAC-запросы.

Spring Boot отвечает за HTTP и конфигурацию, Spring Data JPA — за persistence, а Liquibase является единственным владельцем схемы БД. `open-in-view` отключён, `ddl-auto` работает в режиме `validate`.

## Локальный запуск

Требуются JDK 21+ и Maven. Из корня репозитория:

```powershell
mvn -pl crm-user-service -am install
mvn -pl crm-user-service spring-boot:run
```

Сервис слушает `http://localhost:8081`.

Dev-данные загружаются Liquibase-контекстом `local`. Для запуска без них:

```powershell
$env:LIQUIBASE_CONTEXTS = "prod"
mvn -pl crm-user-service spring-boot:run
```

## API

### Разрешить внутреннего пользователя

```http
POST /internal/v1/user-resolutions
Content-Type: application/json
```

```json
{
  "login": "demo.admin",
  "email": "demo.admin@example.local"
}
```

Операция проверяет, что login/email однозначно соответствуют активному пользователю, но не проверяет роли и permissions. Успех и штатный отказ возвращаются с HTTP 200:

```json
{
  "resolved": true,
  "userId": "11111111-1111-1111-1111-111111111111",
  "userVersion": 1
}
```

```json
{
  "resolved": false,
  "denialReason": "USER_NOT_FOUND"
}
```

### Получить authorization decision

```http
POST /internal/v1/authorization-decisions
Content-Type: application/json
```

Тело запроса:

```json
{
  "login": "demo.admin",
  "email": "demo.admin@example.local",
  "permission": "ORGANIZATION_READ"
}
```

Необходимо передать хотя бы один идентификационный атрибут: `login` или `email`. `permission` обязателен. Login и email нормализуются в lower case, permission — в upper case.

Если переданы оба атрибута, каждый должен существовать и оба должны указывать на одного пользователя.

Положительное решение, HTTP 200:

```json
{
  "allowed": true,
  "userId": "11111111-1111-1111-1111-111111111111",
  "userVersion": 1
}
```

Отказ также является штатным решением с HTTP 200:

```json
{
  "allowed": false,
  "userId": "22222222-2222-2222-2222-222222222222",
  "userVersion": 1,
  "denialReason": "USER_INACTIVE"
}
```

Возможные причины отказа:

| `denialReason` | Значение |
|---|---|
| `USER_NOT_FOUND` | пользователь не найден |
| `IDENTITY_CONFLICT` | login и email не разрешились в одного пользователя |
| `USER_INACTIVE` | пользователь отключён |
| `PERMISSION_NOT_GRANTED` | permission не назначен через роли |

Некорректный запрос возвращает HTTP 400 в формате RFC 9457 `ProblemDetail` с массивом ошибок в поле `errors`.

Пример PowerShell:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8081/internal/v1/authorization-decisions `
  -ContentType application/json `
  -Body '{"login":"demo.admin","permission":"ORGANIZATION_READ"}'
```

Endpoint предназначен только для доверенного внутреннего контура. До появления service-to-service аутентификации его нельзя публиковать напрямую.

## Конфигурация

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `SERVER_PORT` | `8081` | HTTP-порт сервиса |
| `DB_URL` | `jdbc:h2:file:./crm-user;AUTO_SERVER=TRUE` | JDBC URL |
| `DB_USERNAME` | `crm` | пользователь БД |
| `DB_PASSWORD` | `crm` | пароль БД |
| `LIQUIBASE_CONTEXTS` | `local` | активные контексты Liquibase |

Пример PostgreSQL:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/crm_user"
$env:DB_USERNAME = "crm"
$env:DB_PASSWORD = "secret"
```

Файловая H2 использует `AUTO_SERVER=TRUE`, поэтому к ней можно подключаться во время работы приложения. Относительный путь вычисляется от рабочей директории процесса.

## Модель данных

Основная схема:

```text
user --< user_role >-- role --< role_permission >-- permission
  |
  +--< user_group_member >-- user_group (иерархия через parent_id)
```

`user.id` — стабильная UUID identity. Login и email являются изменяемыми атрибутами сопоставления. Поле `version` передаётся downstream вместе с UUID и позволяет различать версии identity.

## Проверка

Из корня репозитория:

```powershell
mvn -pl crm-user-service -am test
```

При изменении Liquibase проверяйте запуск на чистой БД, чтобы применялась вся цепочка changesets. Для каждого изменения authorization behavior или API-контракта добавляйте отдельный тест.

## Границы текущей реализации

Административные CRUD, массовые назначения ролей, аудит изменений и история пользователя пока не реализованы. Внутренний endpoint не имеет самостоятельной технической аутентификации и должен быть изолирован сетевым контуром.
