# Explore With Me

Проект переведён с монолита `core/main-service` на микросервисную архитектуру Spring Cloud.

## Текущий этап

Этап 2 завершён: домены пользователей, категорий, заявок на участие, событий, подборок, комментариев, рейтингов, подписок и локаций вынесены в `core/user-service`, `core/category-service`, `core/request-service`, `core/event-service`, `core/compilation-service`, `core/comment-service`, `core/rating-service`, `core/subscription-service` и `core/location-service`. Инфраструктура переключена на Eureka, Config Server, Gateway и отдельные базы данных; монолит `core/main-service` удалён.

Этап 3: рекомендательная система на Kafka, Avro и gRPC. `stats/collector` принимает действия пользователей по gRPC и публикует их в `stats.user-actions.v1`; `stats/aggregator` считает отсутствующие пары событий и публикует `stats.events-similarity.v1`; `stats/analyzer` хранит взаимодействия и similarity в `ewm_analyzer` и предоставляет gRPC-методы рекомендаций. `event-service` отправляет `VIEW` при чтении события, получает рейтинг из Analyzer, возвращает рекомендации через `GET /events/recommendations` и принимает лайки через `PUT /events/{eventId}/like` с проверкой факта просмотра; `request-service` отправляет `REGISTER` при создании заявки. Поле `views` удалено из DTO событий, `rating` стал `Double`. gRPC-порты `collector` и `analyzer` динамические (`0`), фактический порт публикуется в метаданных Eureka `gRPC_port`.

`user-service` предоставляет публичный API `/admin/users` и внутренний API `/internal/users` для получения пользователя, краткой записи и пакетной выборки по идентификаторам. `category-service` предоставляет публичные API `/categories` и `/admin/categories`, а также внутренний API `/internal/categories`. `request-service` предоставляет публичные API `/users/{userId}/requests` и `/users/{userId}/events/{eventId}/requests`, а также внутренний API `/internal/requests` для получения заявки, пакетной выборки и количества подтверждённых заявок события. `event-service` предоставляет публичные API `/events`, `/events/recommendations`, `/events/{eventId}/like` и `/admin/events`, приватный API `/users/{userId}/events` и внутренний API `/internal/events`. `compilation-service` предоставляет публичный API `/compilations` и административный API `/admin/compilations`. `comment-service` предоставляет публичные API `/events/{eventId}/comments`, приватные API `/users/{userId}/comments` и `/users/{userId}/events/{eventId}/comments`, административный API `/admin/comments`, а также внутренний API `/internal/comments` для получения количества комментариев. `rating-service` предоставляет публичные API `/users/{userId}/events/{eventId}/like`, `/users/{userId}/events/{eventId}/dislike` и `/users/{userId}/events/{eventId}/rating`, а также внутренний API `/internal/ratings` для получения статистики рейтинга. `subscription-service` предоставляет публичный API `/users/{userId}/subscriptions` для управления подписками, подтверждения, отклонения и просмотра подписчиков/подписок, а также внутренний API `/internal/subscriptions`. `location-service` предоставляет публичный API `/locations` для поиска событий рядом с координатами и административный API `/admin/locations`. Контракты Feign-клиентов находятся в `core/common`; сервисы получают связанные данные через `UserClient`, `CategoryClient`, `EventClient`, `RequestClient`, `RatingClient`, `CommentClient` и `SubscriptionClient`.

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
| `stats/collector` | gRPC-приём действий и запись в Kafka | — |
| `stats/aggregator` | Расчёт similarity событий | — |
| `stats/analyzer` | Хранение рекомендаций и gRPC-ответы | `ewm_analyzer` |

Общие DTO и исключения находятся в `core/common`. Общие зависимости сервисов вынесены в `core/service-parent`.

## Инфраструктура

- `infra/discovery-server`: Eureka, порт `8761`.
- `infra/config-server`: Config Server с native-профилем, конфигурации сгруппированы в `infra/config-server/src/main/resources/config` по каталогам `stats`, `core` и `infra`, внутри которых выделена папка на каждый сервис.
- `infra/gateway-server`: Spring Cloud Gateway, порт `8080`.
- `stats/stats-server`: сервис статистики, порт `9090`.
- Kafka: `localhost:9092`, темы `stats.user-actions.v1` и `stats.events-similarity.v1`.
- `stats/collector`: HTTP `9093`, gRPC динамический (`0`), порт публикуется в Eureka как `gRPC_port`.
- `stats/aggregator`: HTTP `9094`.
- `stats/analyzer`: HTTP `9095`, gRPC динамический (`0`), порт публикуется в Eureka как `gRPC_port`.

Маршруты gateway направлены на сервисы через Eureka. Маршруты `/internal/**` наружу не публикуются.

## Сборка

```bash
mvn clean package -DskipTests
```

## Запуск

Инфраструктура и сервисы запускаются через Eureka и Config Server. Каждый прикладной сервис использует `spring.application.name`, совпадающее с именем его конфигурации в Config Server.

Локальный запуск выполняется командой `./start.sh`: скрипт очищает старый Compose-стек, собирает Maven-артефакты, собирает и поднимает Docker Compose. Публичная точка входа — gateway `http://localhost:8080`; Eureka доступен на `8761`, Config Server — на `8888`, сервис статистики — на `9090`.
