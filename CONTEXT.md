# JobFlow — Context

Ultimo aggiornamento: 2026-11-01

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
- `JobService.createPdfJob(...)` costruisce GeneratePdfWork e Job, assegna `CREATED` e salva il Job.
- `JobController` espone `POST /jobs` e `GET /jobs/{id}`; entrambi restituiscono `JobResponse` (`id`, `name`, `status`) tramite `JobMapper`.
- `JobMapper` (MapStruct, `componentModel = "spring"`) mappa `Job -> JobResponse`; `JobMapperImpl` è generato in compilazione.
- `GET /jobs/{id}`: `JobService.findById` lancia `JobNotFoundException`; `GlobalExceptionHandler` risponde 404 con `APIError` (`errorName=JOB_NOT_FOUND`, `path`, `msg`, `status`, `timestamp`).
- `POST /jobs` usa `ResponseEntity.created(...)`; Location viene costruita per la risorsa creata.
- Test automatici (9, tutti verdi): `JobServiceTest` (Mockito), `JobControllerTest` (`@WebMvcTest` con service mockato: 200, 404, 201 + Location, 400 su validazione) e `JobflowApplicationTests`. Il GET non è ancora stato provato a runtime contro MySQL/Postman.
- Test manuale eseguito con Postman: `POST /jobs` ha restituito **201 Created**, id generato e status `CREATED`.
- Persistenza verificata in MySQL: Job e GeneratePdfWork risultano salvati correttamente tramite cascade.
- Processor, ProcessorRegistry, JobWorker ed Executor non sono ancora implementati.

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

**Dispatch asincrono in-process: Executor + JobWorker.**

`GET /jobs/{id}` è implementato e coperto da test MockMvc. Resta una verifica manuale a runtime (Postman + MySQL) su id esistente e inesistente.

Prossima azione: far transitare un Job `CREATED -> PROCESSING -> COMPLETED | FAILED` fuori dal thread della request. Domande aperte da decidere (tutor mode):

1. Chi avvia il worker e quando rispetto al commit della transazione di `POST /jobs`.
2. Quale classe persiste i cambi di stato (worker o metodi di `JobService`).
3. Come si risolve il `Processor` dal tipo concreto di `Work` (`ProcessorRegistry`, senza `if`/`switch`).
4. Dove si cattura l'errore per portare il Job a `FAILED`.
5. `@Async` o bean `ThreadPoolTaskExecutor`/`ExecutorService`.

Primo incremento suggerito: un solo passaggio `CREATED -> PROCESSING -> COMPLETED` con un processor finto, prima del PDF vero. Redis può essere affiancato come studio mirato senza interrompere il Sprint Goal.

## Metodo di lavoro

Tutor mode: prima ragionamento di Fabio, poi indizi progressivi. Evitare implementazioni complete copia-incolla salvo richiesta esplicita. Una sola attività importante in corso. Repository reale > Notion > memoria.
