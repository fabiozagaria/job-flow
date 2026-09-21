# JobFlow

JobFlow è un progetto full stack orientato all'elaborazione asincrona di job. L'obiettivo non è costruire un altro CRUD, ma studiare progressivamente worker, concorrenza, aggiornamenti realtime, retry, idempotenza e messaging.

## Sprint Goal #1

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Primo vertical slice

Il primo tipo di job scelto è la **generazione di un documento PDF**. Il PDF è il workload iniziale con cui verificare l'infrastruttura asincrona; non è il dominio centrale del progetto.

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
genera PDF
  |
COMPLETED ----> PDF disponibile
     \
      -> FAILED + error
```

## Modello concettuale corrente

```text
Job
├── id
├── name
├── type
├── status
├── payload
└── error
```

Stati iniziali:

```text
CREATED -> PROCESSING -> COMPLETED
                    \-> FAILED
```

Per la v0.1, `CREATED` significa che il job è stato accettato ed è in attesa di elaborazione. Non viene introdotto `QUEUED` finché non serve distinguere realmente i due concetti.

`type` identifica il lavoro da eseguire, ad esempio `GENERATE_PDF`. Il payload contiene invece i dati specifici necessari a quel tipo di job. Per esempio un job PDF potrà usare un oggetto dedicato come `GeneratePdfPayload`.

La coda e il worker hanno responsabilità diverse: la coda conserva il lavoro in attesa; il worker prende ed esegue il lavoro. Nella prima versione non è necessario introdurre subito RabbitMQ.

## API e realtime

La creazione del job deve essere asincrona: l'API accetta il lavoro e restituisce rapidamente il `jobId`, senza eseguire l'elaborazione pesante nel controller.

Lo stato di un job potrà essere letto tramite:

```http
GET /jobs/{id}
```

Per gli aggiornamenti realtime verrà valutato inizialmente SSE, dato che il flusso principale è server -> client. WebSocket resta un'opzione se emergerà una reale necessità bidirezionale.

## Roadmap tecnica

1. Job domain + lifecycle + worker in-process.
2. Generazione PDF come primo workload reale.
3. Output del job e gestione errori.
4. Aggiornamenti realtime.
5. Concorrenza, retry, timeout, cancellazione e idempotenza.
6. Messaging/RabbitMQ e, se giustificato, separazione API/worker.
7. Spring AI/tool calling come orchestratore vincolato, non come semplice chatbot.

## Principi

- Piccoli vertical slice funzionanti.
- Una sola attività importante in corso.
- Niente infrastruttura introdotta solo per curriculum.
- Il database persiste lo stato; il client parla con il backend, mai direttamente con il database.
- API, worker, queue e realtime channel hanno responsabilità distinte.
- Ogni nuova tecnologia deve risolvere un problema già visibile nel progetto.

## Stato

**In sviluppo — fase di progettazione del dominio.**

Prossima decisione: modellare il risultato/output prodotto da un job completato e poi trasformare il modello concettuale in una prima implementazione minima.
