# JobFlow — Context

Ultimo aggiornamento: 2026-11-17 (allineamento a PostgreSQL, Swagger UI, Docker e Render)

## Obiettivo

JobFlow è il progetto portfolio principale successivo a Expense Tracker. Deve mostrare capacità di progettare sistemi asincroni e ragionare oltre il CRUD tradizionale.

## Sprint Goal corrente

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Stato reale verificato

- Progetto Spring Boot inizializzato con Maven, Spring Boot 4.1.1 e Java 21 come target.
- Dipendenze: Spring Web MVC, Spring Data JPA, Validation, PostgreSQL Driver, Flyway, springdoc-openapi 3.2.0 (Swagger UI), Lombok e MapStruct 1.6.3 (con `lombok-mapstruct-binding`).
- PostgreSQL configurato su database `job_flow` (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` con default locali; password da `DB_PASSWORD`, non versionata). Dal 17 novembre 2026 sostituisce MySQL. Schema gestito da Flyway (`db/migration/V1__init_schema.sql`), Hibernate in `validate`.
- `Job`: entity con `id`, `name`, `Work`, `Result` e `StatusJob`.
- `StatusJob`: `CREATED`, `PROCESSING`, `COMPLETED`, `FAILED`, persistito con `EnumType.STRING`.
- `Work`: entity astratta con ereditarietà `JOINED`.
- Primo Work concreto: `GeneratePdfWork` con `title` e `textBody`.
- `Result`: entity astratta con ereditarietà `JOINED`; risultati concreti non ancora implementati.
- Job → Work e Job → Result sono associazioni unidirezionali `@OneToOne`.
- Job → Work usa `CascadeType.PERSIST`.
- `JobRepository extends JpaRepository<Job, Long>` implementato.
- `CreatePdfJobRequest` implementato come record con Bean Validation.
- `JobService.createPdfJob(...)` costruisce GeneratePdfWork e Job, assegna `CREATED`, salva il Job e pubblica `JobCreatedEvent(jobId)` dentro la transazione.
- `JobWorker` è un bean con `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` su `handleJobCreated`: parte solo dopo il commit e gira su un thread separato (verificato: `task-1`). `SpringConfig` abilita `@EnableAsync`.
- `JobService.markAsProcessing(long jobId)` (`@Transactional`): legge il Job, lancia `RuntimeException` (messaggio con id e stato) se non è `CREATED`, altrimenti lo porta a `PROCESSING` (dirty checking, nessuna `save`). Il worker la chiama. Verificato a runtime il 1 novembre 2026 (allora su MySQL): dopo `POST /jobs` lo status è `PROCESSING`.
- `JobController` espone `POST /jobs` e `GET /jobs/{id}`; entrambi restituiscono `JobResponse` (`id`, `name`, `status`) tramite `JobMapper`.
- `JobMapper` (MapStruct, `componentModel = "spring"`) mappa `Job -> JobResponse`; `JobMapperImpl` è generato in compilazione.
- `GET /jobs/{id}`: `JobService.findById` lancia `JobNotFoundException`; `GlobalExceptionHandler` risponde 404 con `APIError` (`errorName=JOB_NOT_FOUND`, `path`, `msg`, `status`, `timestamp`).
- `POST /jobs` usa `ResponseEntity.created(...)`; Location viene costruita per la risorsa creata.
- Test automatici: `JobServiceTest` (5, Mockito, incluso `markAsProcessing_movesCreatedJobToProcessing`), `JobControllerTest` (5, `@WebMvcTest` con service mockato: 200, 404, 201 + Location, 400 su nome vuoto e su titolo troppo lungo) e `JobflowApplicationTests` (`contextLoads`, richiede PostgreSQL). I 10 test senza database sono passati il 17 novembre 2026; `contextLoads` non è stato eseguito in quella verifica. Non esistono un test di `markAsProcessing` su un job non in `CREATED` né un test del listener.
- Test manuale eseguito con Postman: `POST /jobs` ha restituito **201 Created**, id generato e status `CREATED`.
- Persistenza verificata a mano su MySQL (28 settembre 2026, prima della migrazione a PostgreSQL): Job e GeneratePdfWork risultano salvati correttamente tramite cascade.
- `GeneratedPDFResult` (tabella `generated_pdf_results`) esiste come entity, ma nessun codice la produce. `Processor<I, O>` esiste solo come interfaccia; `ProcessorRegistry` e un processor concreto non sono ancora implementati. Il worker non porta ancora il Job a `COMPLETED`/`FAILED`.
- Swagger UI: `OpenApiConfig` (springdoc) descrive l'API; `server.forward-headers-strategy=framework` fa usare gli header del proxy, così «Try it out» e `Location` puntano a https.
- Deploy: `Dockerfile` multi-stage (Java 21), `docker-compose.yml` (postgres:17 + app), `docker-entrypoint.sh` che converte `DATABASE_URL` nell'URL JDBC e Blueprint `render.yaml` (web Docker e database `jobflow-db`, piano free: il database scade dopo 30 giorni). Secondo la scheda del portfolio è online su Render con Swagger UI (`https://jobflow-dbnm.onrender.com/swagger-ui.html`); non riverificato qui.

## Decisioni architetturali stabili

- Il dominio centrale è il **Job**, non il documento PDF.
- Lifecycle v0.1: `CREATED -> PROCESSING -> COMPLETED | FAILED`.
- `CREATED` include inizialmente il significato di "in attesa di elaborazione"; niente `QUEUED` prematuro.
- Il tipo concreto del Work identifica il lavoro; niente coppia ridondante `type + payload`.
- Work = input/cosa va fatto.
- Processor = elaborazione specializzata.
- Result = output prodotto.
- ProcessorRegistry = risolve il Processor compatibile senza spargere `if`/`switch` nel Worker.
- JobWorker = orchestra lifecycle ed esecuzione.
- Queue = eventuale meccanismo di attesa/consegna, separato dal Worker.
- Il Job possiede il nuovo Work nella creazione: `CascadeType.PERSIST` è sufficiente; non usare `CascadeType.ALL` senza una motivazione sul lifecycle.
- PostgreSQL (dal 17 novembre 2026, prima MySQL) è la source of truth dei Job; lo schema è versionato con Flyway.
- Redis può essere studiato e introdotto con una responsabilità concreta (es. cache, TTL, idempotenza/lock quando necessario), non come sostituto del database né solo per curriculum.
- RabbitMQ, microservizi, realtime e Spring AI entrano progressivamente solo quando risolvono un problema concreto.

## Modello corrente

```text
Job
├── id
├── name
├── status
├── work -> Work
└── result -> Result

Work
└── GeneratePdfWork
    ├── title
    └── textBody
```

La rappresentazione dell'errore associato a `FAILED` non è ancora implementata e verrà decisa quando si costruirà quel percorso.

## Primo slice di persistenza (28 settembre 2026, storico)

Primo slice verificato, allora su MySQL:

```text
POST /jobs
    -> DTO validato
    -> JobService
    -> GeneratePdfWork
    -> Job(CREATED)
    -> JobRepository.save(job)
    -> CascadeType.PERSIST
    -> database (allora MySQL, oggi PostgreSQL)
    -> 201 Created + JobResponse
```

Questo incremento è verificato tramite Postman e controllo della persistenza nel database.

## WIP / punto preciso di ripresa

**Completare il ciclo del worker: `PROCESSING -> COMPLETED | FAILED`.**

Risolto in tutor mode: domande 1 (worker `AFTER_COMMIT`), 2 (i cambi di stato stanno in metodi transazionali di `JobService`, non nel worker: su `task-1` non c'è transazione) e 5 (`@Async` per ora, Executor dedicato rimandato).

Da fare, nell'ordine:

1. **Test 2 di `markAsProcessing` (lo scrive Fabio):** Job non in `CREATED` -> `assertThatThrownBy(...)` con `isInstanceOf(...)` e `hasMessageContaining(...)`. Poi prova di rottura: rompere il codice di produzione e controllare che il test fallisca.
2. Metodi paralleli in `JobService` per `COMPLETED` e `FAILED`, con test.
3. Domanda 3: `ProcessorRegistry` per risolvere il `Processor` dal tipo concreto di `Work`, senza `if`/`switch`. Poi un processor finto.
4. Guard idempotente in `markAsProcessing` (decisione del 3 novembre, non implementata): evento `CREATED` con job già in `PROCESSING` → ignorato con log a livello basso; job in `COMPLETED` o `FAILED` → eccezione di incoerenza. Poi un test d'integrazione del listener (evento normale e duplicato).
5. Domanda 4: dove catturare l'errore del worker per portare il Job a `FAILED`. Oggi un'eccezione su `task-1` non la vede nessuno, perché il client ha già ricevuto 201.

Da ricordare: `assertThat(x == y)` senza `.isTrue()`/`.isEqualTo()` non verifica niente, quindi ogni test nuovo va provato rompendo il codice. Secondo la scheda del portfolio del 17 novembre, su PostgreSQL in locale sono verificati `POST /jobs` (201), `GET /jobs/{id}` (200 e 404) e il passaggio asincrono a `PROCESSING`; non riverificato in questa sessione.

Redis può essere affiancato come studio mirato senza interrompere il Sprint Goal.

## Metodo di lavoro

Tutor mode: prima ragionamento di Fabio, poi indizi progressivi. Evitare implementazioni complete copia-incolla salvo richiesta esplicita. Una sola attività importante in corso. Repository reale > Notion > memoria.
