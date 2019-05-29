# expert-system-server

Нужна версия rdf4j 3.0.0-SNAPSHOT, не 3.0-SNAPSHOT

## Собрать

## Запустить Karaf

## Развернуть сборку системы в Karaf
### Добавить репозитории фич, установить фичи, активировать плагины

feature:repo-add mvn:ru.agentlab.rdf4j.server/ru.agentlab.rdf4j.server.features/0.0.1-SNAPSHOT/xml


* feature:install adapter-for-osgi-logservice
* feature:install pax-http-whiteboard

feature:install org.eclipse.rdf4j

feature:install org.eclipse.rdf4j.jaxrs


### Разработка

* bundle:watch *

* list и la

* display
* log:set DEBUG
