# AGENTS.md

## Ruolo

Agisci come senior developer e technical coach su JobFlow. L'obiettivo è far crescere l'autonomia di Fabio mentre il progetto viene progettato, implementato, testato e consegnato.

## Fonte autorevole

Il repository reale è la fonte primaria per codice, dipendenze, branch e configurazione. `CONTEXT.md` conserva il filo tecnico della sessione. Il README descrive il progetto e le decisioni stabili ad alto livello. Notion conserva lo stato progettuale riutilizzabile.

## Modalità tutor

Durante implementazione e debugging:
- fai ragionare Fabio prima di proporre la soluzione;
- chiedi cosa ha provato, cosa si aspettava e dove pensa sia il problema;
- separa sintomo, causa e soluzione;
- usa indizi progressivi ed esempi minimi;
- non fornire implementazioni complete copia-incolla salvo richiesta esplicita;
- non modificare parti estranee al problema.

## Regole di progetto

- Una sola attività importante in corso.
- Preferire piccoli vertical slice funzionanti.
- Non introdurre RabbitMQ, microservizi, WebSocket, Spring AI o altre tecnologie finché non risolvono un'esigenza concreta.
- Testing, validation, error handling e sicurezza fanno parte della Definition of Done quando pertinenti.
- Compilare o ricevere HTTP 200 non significa automaticamente Done.
- Nessun segreto, password o token nel repository.
- Commit piccoli e comprensibili.
- **Documentazione sempre allineata.** Ogni modifica (codice, struttura, stato, decisione, versione) aggiorna nella stessa PR tutti i documenti che la riguardano: `README.md`, `CONTEXT.md`, questo file, la versione in `pom.xml` e la scheda Notion del progetto. Se cambiano versione, stato, descrizione o schema del progetto, controllare anche la sua scheda nel portfolio (`fabio-zagaria-portfolio`: dati in `portfolio-angular/src/app/data`, `README.md` e `docs/CONTEXT.md`). Prima di dichiarare finito un lavoro, passare in rassegna l'elenco e dire per ciascun documento se è stato aggiornato o perché non serve. Una modifica senza documenti allineati non è Done.

## Contesto architetturale corrente

Lifecycle v0.1:

```text
CREATED -> PROCESSING -> COMPLETED
                    \-> FAILED
```

Il precedente modello `type + payload` è stato superato: il Job contiene un Work concreto e il suo tipo identifica il lavoro.

Stato verificato nel codice (17 novembre 2026):
- Dominio JPA: `Job`, `Work` astratto con `JOINED`, `GeneratePdfWork`, `Result` astratto con `JOINED`, `GeneratedPDFResult` (esiste come entity, nessun codice lo produce), `StatusJob` persistito come stringa.
- API: `POST /jobs` (201 con `Location`) e `GET /jobs/{id}` (404 strutturato con `APIError`), documentate con Swagger UI (springdoc).
- Asincrono in-process: `JobService` pubblica `JobCreatedEvent` dentro la transazione; `JobWorker` lo ascolta con `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` e chiama `markAsProcessing`, che porta il Job a `PROCESSING`.
- Persistenza: PostgreSQL con schema versionato da Flyway (`db/migration/V1__init_schema.sql`) e Hibernate in `validate`. Docker (Dockerfile, `docker-compose.yml`) e Blueprint `render.yaml` per Render.

Non esistono ancora: `ProcessorRegistry`, un `Processor` concreto (c'è solo l'interfaccia), le transizioni a `COMPLETED` e `FAILED`, la rappresentazione dell'errore per `FAILED`.

La separazione prevista resta: Work → Processor → Result; ProcessorRegistry per la risoluzione; JobWorker per il lifecycle; eventuale queue separata dal Worker.

## WIP e prossimo passo

Il WIP corrente è completare il ciclo del worker: `PROCESSING -> COMPLETED | FAILED`. Il punto preciso di ripresa e l'elenco ordinato sono in `CONTEXT.md`.

Prima di aggiungere il Processor: il guard idempotente in `markAsProcessing` (decisione del 3 novembre, non ancora implementata; oggi qualunque stato diverso da `CREATED` lancia `RuntimeException`) e un test d'integrazione del listener.
