# JobFlow

**Versione corrente:** `v0.1.0-alpha.1` (prerelease).

JobFlow è un progetto backend orientato all'elaborazione asincrona di job. Il primo obiettivo è accettare un lavoro, eseguirlo tramite worker ed esporre stato e risultato. L'esecuzione asincrona è il traguardo della v0.1, non una funzionalità già completata.

## Obiettivo della prima versione

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Primo vertical slice

Il primo workload è la **generazione di un documento PDF**. Il PDF serve a verificare l'infrastruttura asincrona: non è il dominio centrale del progetto.

## Modello corrente

```text
Job
├── id
├── name
├── status
├── work
└── result

Work
└── GeneratePdfWork
    ├── title
    └── textBody
```

`Work` e `Result` sono entity astratte persistite con strategia JPA `JOINED`. Il `Job` mantiene associazioni unidirezionali `@OneToOne` verso Work e Result. La relazione Job → Work usa `CascadeType.PERSIST`: un nuovo Work posseduto dal Job viene persistito insieme al Job senza estendere inutilmente il cascade ad altre operazioni.

`StatusJob` è persistito come stringa e contiene `CREATED`, `PROCESSING`, `COMPLETED` e `FAILED`.

Il Work descrive **cosa va fatto**. L'elaborazione sarà responsabilità di un `Processor<I, O>` specializzato; il Worker dovrà orchestrare il lifecycle senza contenere logica specifica della generazione PDF.

## API implementata

Sono presenti gli endpoint di creazione e lettura:

```text
POST /jobs
GET /jobs/{id}
```

La request usa `CreatePdfJobRequest` con Bean Validation. Il backend costruisce `GeneratePdfWork` e `Job`, assegna autonomamente lo stato iniziale `CREATED` e persiste il Job tramite `JobRepository`.

La risposta usa `JobResponse`, include l'id generato, il nome e lo stato, restituisce **201 Created** e un header `Location` riferito alla risorsa creata.

La persistenza è stata verificata manualmente con Postman (28 settembre 2026, allora su MySQL): il Job e il relativo GeneratePdfWork vengono salvati correttamente tramite cascade. Dal 17 novembre 2026 il database è PostgreSQL, con schema versionato da Flyway.

## Flusso obiettivo v0.1

```text
Client
  |
POST /jobs
  |
Job CREATED
  |
risposta rapida con jobId
  |
Worker
  |
PROCESSING
  |
Processor genera PDF
  |
COMPLETED ----> risultato disponibile
     \
      -> FAILED
```

`CREATED` rappresenta anche l'attesa di elaborazione nella prima versione. Non viene introdotto `QUEUED` finché non serve una distinzione reale.

## Stato e prossimo incremento

Il primo slice di creazione e persistenza è verificato. `GET /jobs/{id}` è implementato e coperto da test automatici (200, 404 con `APIError` `JOB_NOT_FOUND`); secondo la scheda del portfolio del 17 novembre è stato verificato anche in locale su PostgreSQL.

Il dispatch asincrono in-process è in piedi: `JobCreatedEvent` dopo il commit e `JobWorker` (`@Async`) portano il Job a `PROCESSING`. Resta da completare il ciclo fino a `COMPLETED` o `FAILED`.

Redis verrà studiato e introdotto solo con una responsabilità concreta; PostgreSQL rimane la source of truth dei Job.

La rappresentazione dell'errore di un Job fallito è intenzionalmente rimandata a quando verrà implementato il percorso `FAILED`.

## Scope v0.1

1. Verifica a runtime di `GET /jobs/{id}` su ID esistente e inesistente: fatta in locale su PostgreSQL secondo la scheda del portfolio; la logica è coperta anche da test automatici.
2. Implementare un Processor PDF concreto.
3. Eseguire il lavoro con Worker/Executor in-process.
4. Gestire `PROCESSING`, `COMPLETED` e `FAILED` e persistere risultato o errore.
5. Recuperare il risultato e verificare il percorso di successo e quello di fallimento.

La v0.1 termina quando un job può essere creato, elaborato e concluso con un esito verificabile. Realtime, retry complessi, cancellazione, Redis, RabbitMQ, separazione API/worker e Spring AI restano opzioni successive, da scegliere soltanto per un problema concreto.

## Implementato e ancora mancante

- Implementati: entity JPA, `GeneratePdfWork`, modello `GeneratedPDFResult`, repository, `POST /jobs`, `GET /jobs/{id}` e `JobMapper` (MapStruct) per `Job -> JobResponse`.
- `GeneratedPDFResult` è un modello persistente, non prova che un PDF venga già generato.
- `JobWorker` ascolta `JobCreatedEvent` dopo il commit (`@Async`) e porta il Job a `PROCESSING`; non esegue ancora lavori: Processor/Executor, `COMPLETED`/`FAILED` e gestione del risultato non sono operativi.
- `GET /jobs/{id}` restituisce un DTO e risponde 404 strutturato se il Job non esiste; la verifica in locale su PostgreSQL è riportata nella scheda del portfolio.
- L'API è documentata con Swagger UI (springdoc). Non è presente un frontend. Non sono ancora implementati autenticazione, Redis o messaging.

## Avvio locale

Richiede Java 21, Maven e PostgreSQL. Con Docker: `DB_PASSWORD=... docker compose up` avvia PostgreSQL 17 e l'API. Senza Docker servono un database `job_flow` su `localhost:5432` e le variabili d'ambiente `DB_PASSWORD` (obbligatoria) e, se diversi dai default, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` (default `postgres`). Non versionare credenziali reali.

```bash
./mvnw spring-boot:run
```

Su Windows usare `mvnw.cmd`. L'API usa la porta predefinita Spring Boot, `8080`. Lo schema è creato dalle migrazioni Flyway (`src/main/resources/db/migration`) e Hibernate lo valida (`ddl-auto=validate`). Swagger UI è su `/swagger-ui.html`. Il Blueprint `render.yaml` descrive il deploy su Render.

### Database su Neon

Dal 19 novembre 2026 il database di produzione è PostgreSQL su [Neon](https://neon.com) (piano Free: 100 ore di calcolo al mese e 1 GB per progetto), al posto del PostgreSQL Free di Render, che scadeva dopo 30 giorni. Progetto Neon `jobflow` (`cool-band-65912836`, regione `aws-eu-central-1`), branch `main`, PostgreSQL 18.

Su Render, nel servizio `jobflow`:

1. `DATABASE_URL`: la connessione **diretta** di Neon (Connect → pooled connection spenta; l'host non contiene `-pooler`, perché Neon sconsiglia il pooler per Flyway), nel formato `postgresql://utente:password@host/neondb?...` (scheda "Connection string" di Connect, **non** la scheda Java/JDBC: un URL che non inizia con `postgresql://` ferma l'avvio con un messaggio chiaro). `docker-entrypoint.sh` la converte nell'URL JDBC togliendo i parametri e ne ricava `DB_USER` e `DB_PASSWORD`, se non sono impostati a parte.
2. `SPRING_PROFILES_ACTIVE=neon`, da impostare per ultima: `application-neon.properties` impone TLS con verifica del certificato (`sslmode=verify-full`, `channelBinding=require`) e usa al massimo 3 connessioni senza tenerne aperte a riposo, così Neon si spegne quando non è usato.
3. Al primo avvio Flyway crea lo schema. Verificare Swagger UI, un `POST /jobs` e il relativo `GET` dopo un riavvio del servizio.

`neon.ts` descrive la configurazione Neon come codice e `package.json` contiene solo le librerie lette dalla CLI `neon` (`@neon/config`, `@neon/env`): non entrano nella build Maven né nell'immagine Docker. Dalla radice del repository, dopo `npm install` e `neon login`, `neon deploy` scrive in `.env.local` (escluso da Git) `DATABASE_URL` e `DATABASE_URL_UNPOOLED`. Da un PC con browser `psql -h pg.neon.tech` permette di collegarsi senza password per controllare le tabelle.

## Verifiche

```bash
./mvnw test
```

Il repository contiene 11 test: avvio del contesto (`contextLoads`, richiede PostgreSQL), `JobServiceTest` (5, Mockito) e `JobControllerTest` (5, `@WebMvcTest`, service mockato). I 10 test senza database sono passati il 17 novembre 2026. La verifica manuale di creazione/persistenza è documentata; mancano test per repository, worker, listener e per `markAsProcessing` su un job non in `CREATED`.

## Principi

- Piccoli vertical slice funzionanti.
- Una sola attività importante in corso.
- Niente infrastruttura introdotta solo per curriculum.
- Il database persiste lo stato; il client parla con il backend, mai direttamente con il database.
- API, worker, queue e realtime channel hanno responsabilità distinte.
- Ogni nuova tecnologia deve risolvere un problema già visibile nel progetto.

## Stato

**In sviluppo — creazione, persistenza e lettura del Job, passaggio asincrono a `PROCESSING`, Swagger UI e deploy su Render (database su Neon) implementati.**

Prossimo passo: guard idempotente in `markAsProcessing`, poi Processor PDF, `ProcessorRegistry` e transizioni a `COMPLETED`/`FAILED`.
