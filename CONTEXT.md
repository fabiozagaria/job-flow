# JobFlow — Context

Ultimo aggiornamento: 2026-09-28

## Obiettivo

JobFlow è il progetto portfolio principale successivo a Expense Tracker. Deve mostrare capacità di progettare sistemi asincroni e ragionare oltre il CRUD tradizionale.

## Sprint Goal corrente

Un client crea un job e JobFlow riesce a elaborarlo asincronamente fino a uno stato terminale verificabile.

## Stato reale del repository

- Progetto Spring Boot inizializzato con Maven, Spring Boot 4.1.1 e Java 21 come target.
- Dipendenze correnti: Spring Web MVC, Spring Data JPA, Validation, MySQL Driver e Lombok.
- MySQL configurato su database `job_flow`; la password arriva da variabile d'ambiente `DB_PASSWORD` e non è versionata.
- `Job` è una entity con `id`, `name`, `Work`, `Result` e `StatusJob`.
- `StatusJob` usa `EnumType.STRING`: `CREATED`, `PROCESSING`, `COMPLETED`, `FAILED`.
- `Work` è una entity astratta con ereditarietà `JOINED`.
- Primo Work concreto: `GeneratePdfWork` con `title` e `textBody`.
- `Result` è attualmente una entity astratta con ereditarietà `JOINED`; i risultati concreti non sono ancora implementati.
- Le relazioni Job → Work e Job → Result sono unidirezionali `@OneToOne`.
- Repository, Service, Controller, Processor, Registry, Worker ed Executor non sono ancora implementati.

## Decisioni architetturali stabili

- Il dominio centrale è il **Job**, non il documento PDF.
- Lifecycle v0.1: `CREATED -> PROCESSING -> COMPLETED | FAILED`.
- `CREATED` include inizialmente il significato di "in attesa di elaborazione"; niente `QUEUED` prematuro.
- La precedente coppia `type + payload` è stata superata: il tipo concreto del Work identifica già il lavoro.
- Work = input/cosa va fatto.
- Processor = elaborazione specializzata.
- Result = output prodotto.
- ProcessorRegistry = risolve il Processor compatibile senza spargere `if`/`switch` nel Worker.
- JobWorker = orchestra lifecycle ed esecuzione.
- Queue = eventuale meccanismo di attesa/consegna, separato dal Worker.
- RabbitMQ, microservizi, realtime e Spring AI non entrano finché il vertical slice in-process non rende concreta la necessità.

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

## WIP corrente

**Persistenza del primo Job con GeneratePdfWork.**

Prima di progettare ulteriormente Worker/Executor, verificare concretamente il mapping JPA: creare un Job con un nuovo GeneratePdfWork, salvarlo in MySQL e rileggerlo.

Decisione immediata da prendere: se il Job possiede il lifecycle del Work, valutare `CascadeType.PERSIST`; alternativa: persistere esplicitamente il Work prima del Job. Non applicare `CascadeType.ALL` automaticamente senza una motivazione sul lifecycle.

## Passi successivi

Dopo la persistenza verificata:
1. repository/service/API minima per creare e leggere Job;
2. dispatch in-process con Executor e JobWorker;
3. ProcessorRegistry e Processor PDF;
4. Result concreto e percorso COMPLETED/FAILED;
5. realtime e robustezza progressivamente.

## Metodo di lavoro

Tutor mode: prima ragionamento di Fabio, poi indizi progressivi. Evitare implementazioni complete copia-incolla salvo richiesta esplicita. Una sola attività importante in corso. Repository reale > Notion > memoria.
