# Пользователь CRM во внутреннем downstream-сервисе

Сценарий: сервис получил `HttpServletRequest` от gateway и хочет связать бизнес-операцию с пользователем CRM. Если сервис хранит собственную копию профиля, её можно обновлять по версии, переданной gateway.

## 1. Подключить чтение идентификации из заголовков

Для этого нужна отдельная библиотека [internal-identity](../../internal-identity/README.md), которая не зависит от Spring или Servlet API:

```xml
<dependency>
    <groupId>com.spimex</groupId>
    <artifactId>internal-identity</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

В обработчике запроса:

```java
InternalIdentity identity = IdentityHeaders.read(request::getHeader);
UUID userId = identity.getUserId();
long userVersion = identity.getUserVersion();
```

Gateway передаёт стабильный UUID пользователя и его версию в CRM. ФИО, login и email в этих заголовках нет. Пример использует `jakarta.servlet.http.HttpServletRequest`; для `javax.servlet.http.HttpServletRequest` вызов чтения такой же.

Downstream должен принимать такие запросы только от доверенного gateway: библиотека проверяет формат заголовков, но не подлинность отправителя. Если заголовки отсутствуют или некорректны, `IdentityHeaders.read` бросает `IdentityHeaderException`; HTTP-слой должен завершить запрос с 403.

## 2. Определить, нужен ли локальный профиль

Если сервису достаточно знать автора заявки, документа или другой операции, можно сохранить только `userId`. Запрос профиля, локальная копия пользователя и сравнение версий в этом случае не нужны.

Если сервис хранит и использует ФИО, email или другие поля пользователя, ему может понадобиться локальная копия профиля. Тогда рядом с полями сохраняется версия профиля CRM. Это отдельное поле, а не локальная JPA `@Version`.

Сравнение с версией заголовка позволяет избежать лишних HTTP-запросов:

- Локального пользователя нет — загрузить профиль.
- Локальная версия меньше версии заголовка — загрузить профиль заново.
- Локальная версия равна или больше — обновление для этого запроса не требуется. Большая версия возможна при запоздавшем запросе.

Такая сверка показывает актуальность относительно версии, которую видел gateway. Это не фоновая синхронизация всех пользователей и не гарантия, что профиль не изменился после проверки. Если локальная копия не нужна, профиль можно получать непосредственно для конкретной операции без сравнения версий.

## 3. При необходимости загрузить профиль через клиент

Для HTTP-запроса нужна [user-service-client](../README.md):

```xml
<dependency>
    <groupId>com.spimex</groupId>
    <artifactId>user-service-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Клиент настраивается один раз при запуске сервиса. Адрес берётся из конфигурации приложения:

```java
OkHttpClient http = new OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(2))
    .readTimeout(Duration.ofSeconds(3))
    .build();

CrmUserServiceClient userClient =
    new OkHttpCrmUserServiceClient(userServiceBaseUrl, http);

UserResponse user = userClient.getUser(userId);
```

Здесь используются `okhttp3.OkHttpClient`, `java.time.Duration`, `com.spimex.user.client.CrmUserServiceClient`, `com.spimex.user.client.OkHttpCrmUserServiceClient` и `com.spimex.user.client.dto.UserResponse`. Вызывающее приложение управляет жизненным циклом `OkHttpClient` и переиспользует его.

`getUser` вызывает `GET /users/{userId}` и возвращает `id`, `login`, `email`, `fio`, `shortName`, `active`, `version`. Login и email могут быть `null`. Метод возвращает также неактивных пользователей: получение профиля не является проверкой разрешения на бизнес-операцию.

## 4. При необходимости обновить свою БД

Сохраняйте нужные поля и **версию из ответа `getUser`**, которая может быть новее версии заголовка. Создание или обновление должно быть атомарным: уникальность UUID CRM защищает от дублей, а проверка версии при записи — от перезаписи свежих данных параллельным запросом со старым профилем.

Ниже полный flow с абстрактным локальным сервисом без реализации БД. `main` — демонстрационный метод с входным запросом и зависимостями, а не стандартная JVM-точка входа `main(String[])`. В приложении чтение запроса остаётся в controller/filter, а сверка и синхронизация выполняются в application service.

```java
import com.spimex.identity.IdentityHeaders;
import com.spimex.identity.InternalIdentity;
import com.spimex.user.client.CrmUserServiceClient;
import com.spimex.user.client.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public class DownstreamUserExample {

    public static void main(
        HttpServletRequest request,
        LocalUserService userService,
        CrmUserServiceClient userClient
    ) {
        // 1. В downstream пришёл запрос от доверенного gateway.

        // 2. Парсим внутренние заголовки через библиотеку.
        // При отсутствующих/некорректных заголовках библиотека бросит
        // IdentityHeaderException: HTTP-обработчик должен вернуть 403.
        InternalIdentity identity = IdentityHeaders.read(request::getHeader);

        // 3. Получаем первичную информацию: стабильный UUID и версию в CRM.
        UUID userId = identity.getUserId();
        long incomingVersion = identity.getUserVersion();

        // 4. Ищем пользователя в своей БД и сравниваем версии.
        LocalUser localUser = userService.findByCrmId(userId);

        // Равная версия означает, что обновление не требуется.
        // Большая локальная версия означает, что ранее этого запроса пришёл другой
        // и успешно обновил пользователя
        if (localUser != null && localUser.crmVersion >= incomingVersion)
            return;

        // 5. Пользователя нет или локальная версия устарела:
        // запрашиваем актуальный профиль через user-service-client.
        // Реальный вызов библиотеки: GET /users/{userId}.
        UserResponse actualUser = userClient.getUser(userId);

        // Между обработкой в gateway и этим чтением данные могли измениться.
        // Если источник вернул версию старее той, которую уже видел gateway,
        // обновлять локальную копию и продолжать операцию нельзя.
        if (actualUser.getVersion() < incomingVersion)
            throw new IllegalStateException("CRM вернул устаревшую версию пользователя");

        // 6. Создаём или обновляем пользователя в своей БД.
        // Сохраняем поля профиля и actualUser.getVersion(),
        // поскольку версия ответа может быть новее версии заголовка.
        userService.saveIfNewer(actualUser);

        // Теперь можно продолжить бизнес-операцию и связать её с userId.
        //
        // Ошибки getUser (404, недоступность, некорректный ответ) должны
        // обрабатываться HTTP-слоем сервиса: синхронизация не состоялась.
        // Здесь исключения намеренно уходят вызывающему коду.
    }

    // Абстрактный сервис. Реализация и работа с БД за рамками примера.
    public interface LocalUserService {

        // Возвращает null, если локальной записи пока нет.
        LocalUser findByCrmId(UUID crmUserId);

        // Атомарно создаёт запись или обновляет только при большей версии.
        // Проверка версии при записи обязательна: параллельный запрос
        // мог уже сохранить более свежие данные после findByCrmId().
        // UUID CRM должен иметь уникальное ограничение в локальной БД.
        void saveIfNewer(UserResponse user);
    }

    public static final class LocalUser {

        // Версия профиля CRM, сохранённая вместе с его полями.
        // Это полноценное поле в БД, не локальная JPA @Version вашего сервиса.
        private final long crmVersion;

        public LocalUser(long crmVersion) {
            this.crmVersion = crmVersion;
        }
    }
}
```

## Ошибки загрузки профиля

- `CrmUserServiceHttpException`: ошибочный HTTP-статус; `getStatusCode()` позволяет отдельно обработать 404 — пользователь не найден.
- `CrmUserServiceUnavailableException`: сервис недоступен.
- `CrmUserServiceProtocolException`: ответ некорректен или содержит другой UUID.

Если актуальный профиль необходим для операции, при ошибке загрузки синхронизация не состоялась и операцию следует завершить ошибкой. Если бизнес-сценарий допускает работу с локальной копией, политику такой работы сервис определяет отдельно; не следует помечать старые данные новой версией без успешной загрузки.

В примере исключения передаются вызывающему HTTP-слою. Реализация локального сервиса, транзакции и отображение ошибок в HTTP-ответы остаются ответственностью downstream-сервиса.
