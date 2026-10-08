# user-service-client

Java 11-совместимый клиент внутреннего API `user-service`. Клиент не зависит
от Spring и использует OkHttp. Жизненным циклом `OkHttpClient` управляет вызывающее
приложение, поэтому один экземпляр можно безопасно переиспользовать.

```java
CrmUserServiceClient users = new OkHttpCrmUserServiceClient("http://user-service:8081");

AuthorizationResponse decision = users.authorize(
    new AuthorizationRequest("demo.admin", null, "ORGANIZATION_READ"));

UserResolutionResponse identity = users.resolveUser(
    new UserResolutionRequest("demo.admin", null));
```

`resolveUser` подтверждает существование и активность внутреннего пользователя без проверки permission. Отрицательное разрешение является штатным ответом, а транспортные и протокольные ошибки используют ту же иерархию исключений, что и `authorize`.

## Получение основной информации о пользователе

Существующая библиотека поддерживает Java 11 и использует переиспользуемый
OkHttpClient, жизненным циклом которого управляет вызывающий код.

```java
UserResponse user = users.getUser(UUID.fromString("11111111-1111-1111-1111-111111111111"));
String fullName = user.getFio();
Boolean active = user.getActive();
```

Метод вызывает `GET /users/{userId}`. Ответ содержит `id`,
`login`, `email`, `fio`, `shortName`, `active`, `version`; login/email
могут быть null. Неактивные пользователи также возвращаются.
HTTP 404 (пользователь не найден) и другие ошибочные HTTP-статусы приводят
к `CrmUserServiceHttpException` с `getStatusCode()`.
Недоступность сервиса — `CrmUserServiceUnavailableException`, некорректный
ответ или несовпадающий UUID — `CrmUserServiceProtocolException`.
Null вместо UUID отклоняется до HTTP-запроса.

## Бизнес-применение

[Пользователь CRM во внутреннем downstream-сервисе](<docs/Синхронизация пользователя в downstream-сервисе.md>): чтение заголовков, необязательная сверка версий и обновление локального профиля.
