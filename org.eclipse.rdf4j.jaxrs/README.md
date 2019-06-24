# expert-system-server

Нужна версия rdf4j 3.0.0-SNAPSHOT, не 3.0-SNAPSHOT

## Собрать

## Запустить Karaf

## Развернуть сборку системы в Karaf
### Добавить репозитории фич

* feature:repo-add mvn:ru.agentlab.rdf4j.server/ru.agentlab.rdf4j.server.features/0.0.1-SNAPSHOT/xml

### Установить фичи, активировать плагины

* feature:install org.eclipse.rdf4j
* feature:install org.eclipse.rdf4j.jaxrs


### Разработка

* bundle:watch *

* list и la

* display
* log:set DEBUG

```
type=memory&Repository+ID=test55&Repository+title=&Persist=true&Sync+delay=0&EvaluationStrategyFactory=org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory
```