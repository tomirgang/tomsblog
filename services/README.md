# Services

Microservices der Toms-Blog-Plattform. Jeder Service ist ein eigenständiges Maven-Modul mit hexagonaler Architektur.

## Struktur pro Service

```
services/<name>/
├── pom.xml
└── src/
    ├── main/java/de/tomsblog/<name>/
    │   ├── domain/           # Entities, Value Objects, Domain Events
    │   ├── application/      # Use Cases, Port Interfaces
    │   └── adapter/
    │       ├── inbound/      # REST Controller, Event Listener
    │       └── outbound/     # JPA Repositories, Kafka Producer
    └── test/java/de/tomsblog/<name>/
```

## Services

| Service        | Beschreibung                       | Status  |
| -------------- | ---------------------------------- | ------- |
| `blog-content` | Blog-Posts, Tags, Mehrsprachigkeit | Geplant |
