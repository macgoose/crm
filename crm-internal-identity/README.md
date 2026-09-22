# crm-internal-identity

Небольшая Java 11-библиотека для чтения и формирования доверенной внутренней
идентификации CRM. Она не зависит от Spring, `javax.servlet` или `jakarta.servlet`.

```java
InternalIdentity identity = IdentityHeaders.read(request::getHeader);
UUID userId = identity.getUserId();
```

Ссылка на метод `getHeader` работает и для Servlet API, и для большинства
HTTP-фреймворков. Для карты заголовков можно использовать `headers::get`.

Spring-код также не требует специального адаптера:

```java
InternalIdentity identity = IdentityHeaders.read(httpHeaders::getFirst);
```

Для исходящего запроса `IdentityHeaders.write(identity)` возвращает неизменяемую
`Map<String, String>` с обоими доверенными заголовками.
