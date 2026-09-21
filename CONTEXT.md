# JobFlow — Context

Ultimo aggiornamento: 2026-09-21

## Obiettivo

JobFlow è il progetto portfolio principale successivo a Expense Tracker. Deve mostrare capacità di progettare sistemi asincroni e ragionare oltre il CRUD tradizionale.

## Sprint Goal corrente

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Decisioni prese

- Il dominio centrale è il **Job**, non il documento PDF.
- Lifecycle v0.1: `CREATED -> PROCESSING -> COMPLETED | FAILED`.
- `CREATED` include inizialmente il significato di "in attesa di elaborazione"; niente `QUEUED` prematuro.
- `FAILED` è uno stato; l'informazione sull'errore è separata.
- La precedente coppia `type + payload` è stata rivista: `type` era ridondante se serviva solo a riconoscere il tipo concreto di lavoro.
- Il Job contiene un **Work/input concreto**. Il tipo concreto del Work rappresenta già quale lavoro deve essere svolto.
- Primo Work reale: generazione PDF. Un possibile input è `GeneratePdfWork` con i dati necessari alla generazione.
- Work diversi possono avere modelli dati, dipendenze e risultati completamente diversi.
- Il Work descrive **cosa va fatto**; non deve conoscere necessariamente le dipendenze infrastrutturali necessarie per eseguirlo.
- L'elaborazione è delegata a Processor specifici.
- Contratto concettuale del processor: `Processor<I, O>`, dove `I` è il Work/input e `O` il Result/output.
- Esempio: `GeneratePdfProcessor : Processor<GeneratePdfWork, GeneratePdfResult>`.
- Non viene introdotta per ora una `JobResult` comune: risultati diversi non condividono necessariamente dati o comportamento.
- Il Worker non deve contenere `if`/`switch` per scegliere il processor.
- Un **ProcessorRegistry** indirizza il Work verso il Processor corretto tramite una mappa concettuale `Class<?> -> Processor<?, ?>`.
- La chiave rappresenta la classe concreta dell'input, ad esempio `GeneratePdfWork.class`; non rappresenta l'output.
- La Map eterogenea perde parte dell'informazione generica statica: il punto di risoluzione dei tipi deve restare confinato nel Registry, senza spargere cast/wildcard nel Worker.
- Il **JobWorker** orchestra il lifecycle: prende il Job, passa a `PROCESSING`, chiede al Registry il Processor, avvia `process(work)`, quindi porta il Job a `COMPLETED` oppure `FAILED`.
- Nella v0.1 Spring Boot il Worker può essere un `@Component`. `@Component` lo rende un bean gestito da Spring, ma **non** lo rende automaticamente asincrono.
- L'asincronia e il meccanismo con cui i Job `CREATED` vengono consegnati al Worker restano da progettare.
- Il worker esegue/orchestra il job; una queue conserva i job in attesa: non sono la stessa responsabilità.
- Il controller/API non deve eseguire direttamente un lavoro lungo: accetta il job e risponde rapidamente con il suo identificatore.
- Lettura stato prevista: `GET /jobs/{id}`.
- Per il realtime si preferisce valutare SSE prima di WebSocket perché il bisogno iniziale è soprattutto server -> client.
- RabbitMQ, microservizi e Spring AI non entrano nella prima versione. Saranno introdotti solo quando esiste un problema concreto che li giustifica.

## Modello concettuale corrente

```text
Job
├── id
├── name
├── status
├── work
├── result
└── error

Work / input concreto
        |
        v
Processor<I, O>
        |
        v
Result / output
```

Il dettaglio esatto di come `result` verrà rappresentato/persistito nel Job non è ancora implementato; è stato però chiarito che ogni processor può produrre un tipo di risultato differente.

## Dispatch dei Processor

```text
Job
 |
 v
Work concreto
 |
 | getClass()
 v
ProcessorRegistry
 |
 | Class del Work -> Processor
 v
Processor<I, O>
 |
 | process(input)
 v
Result
```

Il Registry è l'indirizzatore: il Worker gli passa il Work e non decide direttamente quale implementazione usare.

## Worker

```text
CREATED
   |
   v
JobWorker
   |
   +--> PROCESSING
   |
   +--> ProcessorRegistry
   |        |
   |        v
   |     Processor
   |        |
   |        v
   |      Result
   |
   +--> COMPLETED
   |
   \--> FAILED + error
```

Responsabilità:
- **Work**: input/cosa va fatto.
- **Processor**: come viene elaborato uno specifico Work.
- **Result**: output prodotto.
- **ProcessorRegistry**: trova il Processor compatibile.
- **JobWorker**: orchestra esecuzione e lifecycle.
- **Queue**: conserva/consegna lavoro in attesa quando prevista dall'architettura.

## Punto esatto di ripresa

Progettare **come il JobWorker viene eseguito realmente in modo asincrono e come riceve i Job in stato CREATED nella v0.1 in-process**, senza introdurre ancora RabbitMQ.

Domande successive:
- quale meccanismo in-process consegna il Job al Worker;
- come persistere/associare il Result al Job;
- come confinare in modo semplice e sicuro la risoluzione generica nel ProcessorRegistry.

## Evoluzione prevista

v0.1: API + DB + worker/executor in-process + lifecycle + PDF.
Successivamente: realtime, concorrenza, retry/timeout/cancellazione/idempotenza, messaging/RabbitMQ, eventuale separazione API/worker, Spring AI/tool calling.

## Metodo di lavoro

Tutor mode: prima ragionamento di Fabio, poi indizi progressivi. Evitare implementazioni complete copia-incolla salvo richiesta esplicita. Una sola attività importante in corso. Repository reale > Notion > memoria.
