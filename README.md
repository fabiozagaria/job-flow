# JobFlow

JobFlow è un progetto full stack orientato all'elaborazione asincrona di job. L'obiettivo non è costruire un altro CRUD, ma studiare progressivamente worker, concorrenza, aggiornamenti realtime, retry, idempotenza e messaging.

## Sprint goal #1

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

È disponibile il primo endpoint di creazione:

```text
POST /jobs
```

La request usa `CreatePdfJobRequest` con Bean Validation. Il backend costruisce `GeneratePdfWork` e `Job`, assegna autonomamente lo stato iniziale `CREATED` e persiste il Job tramite `JobRepository`.

La risposta usa `JobResponse`, include l'id generato, il nome e lo stato, restituisce **201 Created** e un header `Location` riferito alla risorsa creata.

La persistenza è stata verificata manualmente con Postman e MySQL: il Job e il relativo GeneratePdfWork vengono salvati correttamente tramite cascade.

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

## WIP corrente

Il primo slice di creazione e persistenza è verificato. Il prossimo passo è completare la lettura di un Job con `GET /jobs/{id}`, inclusa la gestione del caso inesistente.

Solo dopo si passa al dispatch asincrono in-process con Executor e JobWorker.

Redis verrà studiato e introdotto solo con una responsabilità concreta; MySQL rimane la source of truth dei Job.

La rappresentazione dell'errore di un Job fallito è intenzionalmente rimandata a quando verrà implementato il percorso `FAILED`.

## Roadmap tecnica

1. Completare GET /jobs/{id} e gestione not-found.
2. Worker/executor in-process, lifecycle ed esecuzione asincrona.
3. Processor PDF e persistenza dell'output.
4. Gestione errori e lettura dello stato.
5. Aggiornamenti realtime, valutando SSE per primo.
6. Concorrenza, retry, timeout, cancellazione e idempotenza.
7. Redis dove risolve un problema concreto, mantenendo MySQL come source of truth.
8. Messaging/RabbitMQ e, se giustificato, separazione API/worker.
9. Spring AI/tool calling come orchestratore vincolato, non come semplice chatbot.

## Principi

- Piccoli vertical slice funzionanti.
- Una sola attività importante in corso.
- Niente infrastruttura introdotta solo per curriculum.
- Il database persiste lo stato; il client parla con il backend, mai direttamente con il database.
- API, worker, queue e realtime channel hanno responsabilità distinte.
- Ogni nuova tecnologia deve risolvere un problema già visibile nel progetto.

## Stato

**In sviluppo — creazione e persistenza del primo Job verificate end-to-end.**

Prossimo passo: implementare e verificare `GET /jobs/{id}`, poi iniziare il dispatch asincrono in-process.
