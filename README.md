# Explore With Me

Проект переводится с монолита `core/main-service` на микросервисную архитектуру Spring Cloud.

## Текущий этап

Этап 2: создан каркас микросервисов, перенесены домены пользователей, категорий и заявок на участие в `core/user-service`, `core/category-service` и `core/request-service`. Остальная бизнес-логика по-прежнему находится в `core/main-service`; перенос domain-кода будет выполняться поэтапно.

`user-service` предоставляет публичный API `/admin/users` и внутренний API `/internal/users` для получения пользователя, краткой записи и пакетной выборки по идентификаторам. `category-service` предоставляет публичные API `/categories` и `/admin/categories`, а также внутренний API `/internal/categories`. `request-service` предоставляет публичные API `/users/{userId}/requests` и `/users/{userId}/events/{eventId}/requests`, а также внутренний API `/internal/requests` для получения заявки, пакетной выборки и количества подтверждённых заявок события. Контракты Feign-клиентов находятся в `core/common`; `request-service` получает пользователей и события через `UserClient` и `EventClient`.

## Модули

| Модуль | Назначение | База данных |
| --- | --- | --- |
| `core/user-service` | Пользователи | `ewm_user` |
| `core/category-service` | Категории | `ewm_category` |
| `core/event-service` | События | `ewm_event` |
| `core/request-service` | Заявки на участие | `ewm_request` |
| `core/compilation-service` | Подборки событий | `ewm_compilation` |
| `core/comment-service` | Комментарии | `ewm_comment` |
| `core/rating-service` | Рейтинги событий | `ewm_rating` |
| `core/subscription-service` | Подписки | `ewm_subscription` |
| `core/location-service` | Локации | `ewm_location` |

Общие DTO и исключения находятся в `core/common`. Общие зависимости сервисов вынесены в `core/service-parent`.

## Инфраструктура

- `infra/discovery-server`: Eureka, порт `8761`.
- `infra/config-server`: Config Server с native-профилем, конфигурации в `infra/config-server/src/main/resources/config`.
- `infra/gateway-server`: Spring Cloud Gateway, порт `8080`.
- `stats/stats-server`: сервис статистики, порт `9090`.

Маршруты gateway направлены на сервисы через Eureka. Маршруты `/internal/**` наружу не публикуются.

## Сборка

```bash
mvn clean package -DskipTests
```

## Запуск

Инфраструктура и сервисы запускаются через Eureka и Config Server. Каждый прикладной сервис использует `spring.application.name`, совпадающее с именем его конфигурации в Config Server.

Локальный запуск через Docker Compose и перевод `main-service` на новые сервисы будут добавлены на следующих этапах.
