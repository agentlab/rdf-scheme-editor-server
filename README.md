# expert-system-server

JAX-RS OSGi HTTP Whiteboard version of RDF4J server (with Sparql Endpoint and other REST API).

## Собрать

Перейти в подкаталог org.eclipse.rdf4j.jaxrs и выполнить из него компиляцию с прогоном тестов

```
mvn clean install
```

Компиляция без прогона тестов (отрабатывает быстрее)

```
mvn clean install -P quick
```

## Запустить Karaf

Из корневой папки бинарников карафа, не из папки bin!

https://karaf.apache.org/get-started.html

## Развернуть сборку системы в Karaf

Перед установкой надо сначала собрать систему (см. раздел "Сборка").

### Добавить репозитории фич

* `feature:repo-add mvn:ru.agentlab.rdf4j/ru.agentlab.rdf4j.features/0.0.1-SNAPSHOT/xml`

### Установить фичи, активировать плагины

Главная фича, которая устанавливает все (если все ок, ее достаточно)

* `feature:install ru.agentlab.rdf4j.jaxrs`

На случай, если ошибки, можно устанавливать последовательно фичи

* `feature:install org.eclipse.rdf4j`
* `feature:install karaf-scr`
* `feature:install karaf-rest-all`
* `feature:install ru.agentlab.rdf4j.jaxrs.deps`
* `feature:install ru.agentlab.rdf4j.jaxrs`

### Разработка

* `bundle:watch *` -- для того, чтобы Karaf следил за локальным репозиторием и переустанавливал бандлы после пересборки

* `bundle:list` и `la` -- список всех плагинов
* `feature:list` -- список всех фич

* `display` -- показать логи
* `log:set DEBUG` -- установка фильтра логов в подробный режим
