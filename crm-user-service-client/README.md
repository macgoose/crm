# crm-user-service-client

Java 11-совместимый клиент внутреннего API `crm-user-service`. Клиент не зависит
от Spring и использует OkHttp. Жизненным циклом `OkHttpClient` управляет вызывающее
приложение, поэтому один экземпляр можно безопасно переиспользовать.

```java
OkHttpClient http = new OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(2))
    .readTimeout(Duration.ofSeconds(3))
    .build();

CrmUserServiceClient users =
    new OkHttpCrmUserServiceClient("http://crm-user-service:8081", http);

AuthorizationResponse decision = users.authorize(
    new AuthorizationRequest("demo.admin", null, "ORGANIZATION_READ"));

UserResolutionResponse identity = users.resolveUser(
    new UserResolutionRequest("demo.admin", null));
```

`resolveUser` подтверждает существование и активность внутреннего пользователя без проверки permission. Отрицательное разрешение является штатным ответом, а транспортные и протокольные ошибки используют ту же иерархию исключений, что и `authorize`.
