# JobFlow

JobFlow è un progetto backend orientato all'elaborazione asincrona di job. Il primo obiettivo è accettare un lavoro, eseguirlo tramite worker ed esporre stato e risultato. L'esecuzione asincrona è il traguardo della v0.1, non una funzionalità già completata.

## Obiettivo della prima versione

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

## Stato e prossimo incremento

Il primo slice di creazione e persistenza è verificato. Il prossimo passo è completare la lettura di un Job con `GET /jobs/{id}`, inclusa la gestione del caso inesistente.

Solo dopo si passa al dispatch asincrono in-process con Executor e JobWorker.

Redis verrà studiato e introdotto solo con una responsabilità concreta; MySQL rimane la source of truth dei Job.

La rappresentazione dell'errore di un Job fallito è intenzionalmente rimandata a quando verrà implementato il percorso `FAILED`.

## Scope v0.1

1. Completare `GET /jobs/{id}`, DTO e caso inesistente.
2. Implementare un Processor PDF concreto.
3. Eseguire il lavoro con Worker/Executor in-process.
4. Gestire `PROCESSING`, `COMPLETED` e `FAILED` e persistere risultato o errore.
5. Recuperare il risultato e verificare il percorso di successo e quello di fallimento.

La v0.1 termina quando un job può essere creato, elaborato e concluso con un esito verificabile. Realtime, retry complessi, cancellazione, Redis, RabbitMQ, separazione API/worker e Spring AI restano opzioni successive, da scegliere soltanto per un problema concreto.

## Implementato e ancora mancante

- Implementati: entity JPA, `GeneratePdfWork`, modello `GeneratedPDFResult`, repository e `POST /jobs`.
- `GeneratedPDFResult` è un modello persistente, non prova che un PDF venga già generato.
- `JobWorker` è ancora vuoto; Processor/Executor e gestione del risultato non sono operativi.
- `GET /jobs/{id}` non è ancora esposto: anche il riferimento `Location` della creazione anticipa quella risorsa.
- Non è presente un frontend. Non sono ancora implementati autenticazione, Redis o messaging.

## Avvio locale

Richiede Java 21, Maven e MySQL con database `job_flow`. La configurazione corrente usa MySQL su `localhost:3306`, utente `root` e password da `DB_PASSWORD`. Impostare i valori tramite ambiente; non versionare credenziali reali.

```bash
./mvnw spring-boot:run
```

Su Windows usare `mvnw.cmd`. L'API usa la porta predefinita Spring Boot, `8080`. Hibernate usa attualmente `ddl-auto=update`, configurazione di sviluppo che non equivale a migrazioni versionate per produzione.

## Verifiche

```bash
./mvnw test
```

Il repository contiene un test di avvio del contesto. La verifica manuale di creazione/persistenza è documentata; manca una suite comportamentale per lettura, worker e lifecycle.

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
