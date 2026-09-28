# JobFlow — Context

Ultimo aggiornamento: 2026-09-28

## Obiettivo

JobFlow è il progetto portfolio principale successivo a Expense Tracker. Deve mostrare capacità di progettare sistemi asincroni e ragionare oltre il CRUD tradizionale.

## Sprint Goal corrente

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Stato reale verificato

- Progetto Spring Boot inizializzato con Maven, Spring Boot 4.1.1 e Java 21 come target.
- Dipendenze: Spring Web MVC, Spring Data JPA, Validation, MySQL Driver e Lombok.
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
- `JobController` espone `POST /jobs` e restituisce `JobResponse` tramite `ResponseEntity.created(...)`.
- La response contiene `id`, `name` e `status`; Location viene costruita per la risorsa creata.
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

**Completare la lettura del Job creato.**

Prossima azione: implementare `GET /jobs/{id}`, partendo da `JobRepository.findById(id)`, decidendo la gestione del caso inesistente e restituendo un DTO appropriato.

Dopo la verifica del GET, passare al dispatch asincrono in-process con Executor e JobWorker. Redis può essere affiancato come studio/integrazione mirata senza interrompere il Sprint Goal.

## Metodo di lavoro

Tutor mode: prima ragionamento di Fabio, poi indizi progressivi. Evitare implementazioni complete copia-incolla salvo richiesta esplicita. Una sola attività importante in corso. Repository reale > Notion > memoria.
