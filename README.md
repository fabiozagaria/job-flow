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

`Work` e `Result` sono entity astratte persistite con strategia JPA `JOINED`. Il `Job` mantiene associazioni unidirezionali `@OneToOne` verso Work e Result. `StatusJob` è persistito come stringa e contiene `CREATED`, `PROCESSING`, `COMPLETED` e `FAILED`.

Il Work descrive **cosa va fatto**. L'elaborazione sarà responsabilità di un `Processor<I, O>` specializzato; il Worker dovrà orchestrare il lifecycle senza contenere logica specifica della generazione PDF.

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

Prima di introdurre Worker ed Executor, il vertical slice deve dimostrare che un `Job` contenente un nuovo `GeneratePdfWork` può essere persistito e riletto correttamente da MySQL.

Il prossimo punto tecnico è decidere il lifecycle di persistenza della relazione Job → Work: salvataggio esplicito del Work oppure cascade dal Job. La scelta deve precedere l'implementazione del primo Repository/Service.

La rappresentazione dell'errore di un Job fallito è intenzionalmente rimandata a quando verrà implementato il percorso `FAILED`.

## Roadmap tecnica

1. Persistenza e rilettura del primo Job con GeneratePdfWork.
2. Worker/executor in-process, lifecycle ed esecuzione asincrona.
3. Processor PDF e persistenza dell'output.
4. Gestione errori e lettura dello stato.
5. Aggiornamenti realtime, valutando SSE per primo.
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

**In sviluppo — bootstrap Spring Boot completato e dominio JPA iniziale implementato.**

Prossimo passo: rendere persistibile e verificare end-to-end il primo `Job` con `GeneratePdfWork`, partendo dalla decisione sul cascade.
