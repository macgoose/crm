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
    }
}