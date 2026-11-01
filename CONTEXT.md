# JobFlow — Context

Ultimo aggiornamento: 2026-11-01 (fine sessione worker)

## Obiettivo

JobFlow è il progetto portfolio principale successivo a Expense Tracker. Deve mostrare capacità di progettare sistemi asincroni e ragionare oltre il CRUD tradizionale.

## Sprint Goal corrente

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Stato reale verificato

- Progetto Spring Boot inizializzato con Maven, Spring Boot 4.1.1 e Java 21 come target.
- Dipendenze: Spring Web MVC, Spring Data JPA, Validation, MySQL Driver, Lombok e MapStruct 1.6.3 (con `lombok-mapstruct-binding`).
- MySQL configurato su database `job_flow`; password da `DB_PASSWORD`, non versionata.
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
- `JobService.markAsProcessing(long jobId)` (`@Transactional`): legge il Job, lancia `RuntimeException` (messaggio con id e stato) se non è `CREATED`, altrimenti lo porta a `PROCESSING` (dirty checking, nessuna `save`). Il worker la chiama. Verificato a runtime: dopo `POST /jobs` lo status in MySQL è `PROCESSING`.
- `JobController` espone `POST /jobs` e `GET /jobs/{id}`; entrambi restituiscono `JobResponse` (`id`, `name`, `status`) tramite `JobMapper`.
- `JobMapper` (MapStruct, `componentModel = "spring"`) mappa `Job -> JobResponse`; `JobMapperImpl` è generato in compilazione.
- `GET /jobs/{id}`: `JobService.findById` lancia `JobNotFoundException`; `GlobalExceptionHandler` risponde 404 con `APIError` (`errorName=JOB_NOT_FOUND`, `path`, `msg`, `status`, `timestamp`).
- `POST /jobs` usa `ResponseEntity.created(...)`; Location viene costruita per la risorsa creata.
- Test automatici (tutti verdi): `JobServiceTest` (Mockito, incluso `markAsProcessing_movesCreatedJobToProcessing`), `JobControllerTest` (`@WebMvcTest` con service mockato: 200, 404, 201 + Location, 400 su validazione) e `JobflowApplicationTests`. Il GET non è ancora stato provato a runtime contro MySQL/Postman.
- Test manuale eseguito con Postman: `POST /jobs` ha restituito **201 Created**, id generato e status `CREATED`.
- Persistenza verificata in MySQL: Job e GeneratePdfWork risultano salvati correttamente tramite cascade.
- `Processor<I, O>` esiste solo come interfaccia; `ProcessorRegistry` e un processor concreto non sono ancora implementati. Il worker non porta ancora il Job a `COMPLETED`/`FAILED`.

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
- MySQL rimane la source of truth dei Job.
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

## Incremento completato oggi

Primo slice di persistenza verificato:

```text
POST /jobs
    -> DTO validato
    -> JobService
    -> GeneratePdfWork
    -> Job(CREATED)
    -> JobRepository.save(job)
    -> CascadeType.PERSIST
    -> MySQL
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
4. Domanda 4: dove catturare l'errore del worker per portare il Job a `FAILED`. Oggi un'eccezione su `task-1` non la vede nessuno, perché il client ha già ricevuto 201.

Da ricordare: `assertThat(x == y)` senza `.isTrue()`/`.isEqualTo()` non verifica niente, quindi ogni test nuovo va provato rompendo il codice. `GET /jobs/{id}` resta da provare a runtime (Postman + MySQL) con id inesistente (404).

Redis può essere affiancato come studio mirato senza interrompere il Sprint Goal.

## Metodo di lavoro

Tutor mode: prima ragionamento di Fabio, poi indizi progressivi. Evitare implementazioni complete copia-incolla salvo richiesta esplicita. Una sola attività importante in corso. Repository reale > Notion > memoria.
