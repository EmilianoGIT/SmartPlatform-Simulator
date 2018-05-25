L'applicazione è in ascolto sulla porta 8080 dell'http.

Il Json di simulazione si presenta nel seguente formato: (da notare, tutte le key hanno value di tipo stringa per adesso, a livello di backend vengono applicati i cast ai campi più importanti)

{
   "simStartDate": "",     //2018-05-25T17:46:52.496+02:00
   "simDuration": "",       //hh:mm:ss 
   "scenario": {
      "sceName": "",        
      "sensors": [
         {
            "senName": "",
            "type": "",
            "polling": "",     //espresso in secondi (intero)
            "ref": "",
            "topic": "",
            "models": [
               {
                  "modName": "",
                  "cat": "",
                  "probability": "",       //esprimibile in decimale o intero
                  "measures": [
                     {
                        "meaName": "",
                        "key": "",
                        "unity": "",
                        "min": "",
                        "max": "",
                        "source": "",       
                        "destination": "",
                        "prob": "",
                        "variance": "",
                        "selArray": ""
                     }
                  ]
               }
            ]
         }
      ]
   }
}

che viene poi passato all' api "api/engine/start_simulation" (POST).

Precisazioni:
-se "simStartDate" è "" o ha data vecchia, l'engine parte all'istante.

-"source", "destination" possono rimanere "".

-"selArray" deve avere come stringa o "M" o "R".

-la somma delle probabilità dei modelli contenuti in un sensore deve essere 100, se ciò non è vero il json di simulazione non viene preso in considerazione

-ogni sensore deve avere "ref" diversa nello scenario.


E' possibile fermare la simulazione in corso con l'api "api/engine/stop_simulation" (GET)
Per vedere gli snapshots prodotti durante è dopo la simulazione è possibile utilizzare l'api "api/engine/produced_snapshots" (GET), che risponde con un array di snapshots

Un esempio di Json di simulazione:

{
"simStartDate": "2018-05-25T17:46:52.496+02:00",
"simDuration": "00:02:05",
"scenario" : {
"sceName": "scenario bello",
"sensors": [
{
"senName": "sensore1",
"type": "Environmental",
"polling": "3",
"ref": "jzp/01",
"topic" : "tenantX/jzp/0001",
"models": [
{
"modName": "modello di misure nella norma",
"cat": "2033",
"probability": "90.0",
"measures": [
{
"meaName": "temperatura nella norma",
"key": "device_temperature",
"unity": "C°",
"min": "20",
"max": "30",
"source": "",
"destination": "",
"prob": "50",
"variance": "6",
"selArray": "M"
}
]
},
{
"modName": "modello di misure anomale",
"cat": "2033",
"probability": "10.0",
"measures": [
{
"meaName": "temperatura anomala",
"key": "device_temperature",
"unity": "C°",
"min": "100",
"max": "110",
"source": "",
"destination": "",
"prob": "10.0",
"variance": "1.0",
"selArray": "M"
}
]
}
]
}
]
}
}

Questo json contiene i settaggi per una simulazione della durata di 2 minuti e 5 secondi, in cui è presente un solo sensore "Environmental" che emette snapshot ogni 3 secondi, il 90% delle volte viene preso il modello in cui device_temperature è nella norma, per il 10% invece viene segnata una temperatura anomala.

