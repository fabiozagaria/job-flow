# JobFlow

JobFlow è un progetto full stack orientato all'elaborazione asincrona di job. L'obiettivo non è costruire un altro CRUD, ma studiare progressivamente worker, concorrenza, aggiornamenti realtime, retry, idempotenza e messaging.

## Sprint goal #1

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Primo vertical slice

Il primo workload è la **generazione di un documento PDF**. Il PDF serve a verificare l'infrastruttura asincrona: non è il dominio centrale del progetto.

Flusso concettuale:

```text
Client
  |
POST /jobs
  |
Job CREATED
  |
risposta immediata con jobId
  |
Worker
  |
PROCESSING
  |
Processor genera PDF
  |
COMPLETED ----> PDF disponibile
     \
      -> FAILED + errore
```

## Modello concettuale corrente

```text
Job
├── id
├── name
├── status
├── work
├── result
└── error

Work concreto
        |
        v
Processor<I, O>
        |
        v
Result concreto
```

Il Job contiene un **Work concreto**, cioè l'input che descrive cosa va fatto. Il tipo concreto del Work identifica il lavoro: per esempio, `GeneratePdfWork`.

Il Work non esegue il lavoro e non deve conoscere le dipendenze infrastrutturali. L'elaborazione è responsabilità di un `Processor<I, O>` specializzato, ad esempio `GeneratePdfProcessor`.

Un `ProcessorRegistry` associa la classe concreta del Work al Processor compatibile. Il `JobWorker` orchestra il lifecycle: riceve un Job, lo porta a `PROCESSING`, risolve il Processor, lo esegue e porta il Job a `COMPLETED` o `FAILED`.

Stati iniziali:

```text
CREATED -> PROCESSING -> COMPLETED
                    \-> FAILED
```

Nella v0.1, `CREATED` significa che il job è stato accettato ed è in attesa di elaborazione. Non viene introdotto `QUEUED` finché non serve distinguere realmente i due concetti.

La queue e il worker hanno responsabilità diverse: la prima conserva o consegna lavoro in attesa; il secondo lo esegue. Nella prima versione non è necessario introdurre RabbitMQ.

## API e realtime

La creazione del job deve essere asincrona: l'API accetta il lavoro e restituisce rapidamente il `jobId`, senza eseguire l'elaborazione pesante nel controller.

Lo stato di un job sarà consultabile tramite:

```http
GET /jobs/{id}
```

Per gli aggiornamenti realtime verrà valutato inizialmente SSE, dato che il flusso principale è server -> client. WebSocket resta un'opzione se emergerà una reale necessità bidirezionale.

## Roadmap tecnica

1. Job domain e persistenza del primo Work.
2. Worker/executor in-process, lifecycle ed esecuzione asincrona.
3. Generazione PDF e persistenza dell'output.
4. Gestione errori e lettura dello stato.
5. Aggiornamenti realtime.
6. Concorrenza, retry, timeout, cancellazione e idempotenza.
7. Messaging/RabbitMQ e, se giustificato, separazione API/worker.
8. Spring AI/tool calling come orchestratore vincolato, non come semplice chatbot.

## Principi

- Piccoli vertical slice funzionanti.
- Una sola attività importante in corso.
- Niente infrastruttura introdotta solo per curriculum.
- Il database persiste lo stato; il client parla con il backend, mai direttamente con il database.
- API, worker, queue e realtime channel hanno responsabilità distinte.
- Ogni nuova tecnologia deve risolvere un problema già visibile nel progetto.

## Stato

**In sviluppo — dominio e primo Work modellati.**

Prossimo passo: progettare come i Job in stato `CREATED` vengono consegnati ed eseguiti realmente in modo asincrono dal JobWorker nella v0.1 in-process, senza introdurre RabbitMQ prematuramente.
