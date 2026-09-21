# JobFlow — Context

Ultimo aggiornamento: 2026-09-21

## Obiettivo

JobFlow è il progetto portfolio principale successivo a Expense Tracker. Deve mostrare capacità di progettare sistemi asincroni e ragionare oltre il CRUD tradizionale.

## Sprint Goal corrente

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Decisioni prese

- Il dominio centrale è il **Job**, non il documento PDF.
- Modello minimo corrente: `id`, `name`, `type`, `status`, `payload`, `error`.
- Stati v0.1: `CREATED -> PROCESSING -> COMPLETED | FAILED`.
- `CREATED` include inizialmente il significato di "in attesa di elaborazione"; niente `QUEUED` prematuro.
- `FAILED` è uno stato; l'informazione sull'errore è separata.
- `type` discrimina il tipo di elaborazione.
- Il `payload` è un oggetto specifico del tipo di job. Per `GENERATE_PDF` si prevede un oggetto dedicato, ad esempio `GeneratePdfPayload`.
- Il worker esegue il job. Una queue conserva i job in attesa: non sono la stessa responsabilità.
- Il controller/API non deve eseguire direttamente un lavoro lungo: accetta il job e risponde rapidamente con il suo identificatore.
- Lettura stato prevista: `GET /jobs/{id}`.
- Per il realtime si preferisce valutare SSE prima di WebSocket perché il bisogno iniziale è soprattutto server -> client.
- Primo workload reale: elaborazione/generazione di un **PDF**.
- RabbitMQ, microservizi e Spring AI non entrano nella prima versione. Saranno introdotti solo quando esiste un problema concreto che li giustifica.

## Flusso corrente

```text
POST /jobs
    |
    v
CREATED
    |
    v
Worker
    |
    v
PROCESSING
   / \
  v   v
COMPLETED  FAILED
    |
    v
PDF/output disponibile
```

## Punto esatto di ripresa

La domanda aperta è: **come modellare il riferimento al risultato prodotto dal job?**

Opzioni ancora da ragionare:
- output dentro `Job`;
- output dentro il payload;
- concetto/oggetto separato per il risultato.

Non scegliere automaticamente una soluzione: Fabio deve ragionare prima sui pro e contro.

## Evoluzione prevista

v0.1: API + DB + worker/executor in-process + lifecycle + PDF.
Successivamente: realtime, concorrenza, retry/timeout/cancellazione/idempotenza, messaging/RabbitMQ, eventuale separazione API/worker, Spring AI/tool calling.

## Metodo di lavoro

Tutor mode: prima ragionamento di Fabio, poi indizi progressivi. Evitare implementazioni complete copia-incolla salvo richiesta esplicita. Una sola attività importante in corso. Repository reale > Notion > memoria.
