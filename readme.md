# BackEnd di simulazione dati snapshot

L'applicazione permette di gestire simulazioni basate su scenari composti da sensori che producono snapshots in base alla configurazione di modelli di misurazioni.
E' possibile impostare una data di inizio simulazione o farle partire al momento. Ogni simulazione ha una durata che è stabilita dall'utente
I valori delle misurazioni si possono basare su:

1)-Una serie di valori predefiniti ("v", "p", "l")

2)- Andamenti nel tempo contenuti in un range, tra cui: lineare crescente, lineare descrescente, esponenziale crescente, esponenziale decrescente, sinusoidale, cosinusoidale, e gaussiana.

3)- Random generato da range (senza un criterio)

## Getting Started
Per scaricare il progetto è necessario fare una clone dall'IDE (come IntelliJ) al seguente link:
`https://emiliano.barigelli@git.eimware.it/r/smartplatform/sp-simulator.git`
### Prerequisites
```
Java: JDK,JRE
```
### Installing
1)- Nel caso in cui non abbiate Java, indirizzarsi a: `https://www.java.com/it/download/` e procedere con il setup guidato.

2)- Nella root del progetto trovate il file "config.properties" che consente la configurazione dei seguenti parametri:
```
-server_rest_port (porta dall'applicazione in ascolto per le richieste HTTP)
```
```
-mqtt_broker_address (indirizzo che fa riferimento all'host con il brokerMQTT)
```
```
-mqtt_broker_port (porta del brokerMQTT, che è la 1883 di default)
```
Se il brokerMQTT è configurato con l'autenticazione impostare nome utente e passoword, altrimenti lasciare i campi vuoti
```
-mqtt_broker_username 
```
```
-mqtt_broker_password 
```
```
-mqtt_broker_use_ssl (specificare se il brokerMQTT utilizza Secure Socket Layer, scrivere true o false)
```
Un esempio di configurazione (caso in cui il brokerMQTT è in locale e non vi è ne autenticazione ne SSL):
```
-server_rest_port=8000
```
```
-mqtt_broker_address=localhost
```
```
-mqtt_broker_port=1883
```
```
-mqtt_broker_username=
```
```
-mqtt_broker_password=
```
```
-mqtt_broker_use_ssl=false
```
Una volta configurato procedete con l'avvio tramite la classe "Main".

a)- API er creare una simulazione
```
POST "/api/v1.0/simulator/new_simulation"
```
Esempio per il JSON di simulazione

```
{  
```
```   
     "simStartDate":"2018-10-04T20:02:00",
```
```
     "simDuration":"00:01:30",
```
```
     "scenario":{
```
```
        "sceName":"Scenario di prova",
```
```
        "sensors":[  
```
```
           {  
```
```
              "senName":"Environmental1",
```
```
              "ref":"jzp://edv#0001.0000",
```
```
              "topic":"tenantX/jz/device/snapshots/0000/edv/0001",
```
```
              "polling":"1",
```
```
              "type":"Environmental",
```
```
              "models":[ 
```
```
                 {
```
```
                    "modName":"modello1",
```
```
                    "probability":"100",
```
```
                    "cat":"0220",
```
```
                    "measures":[
```
```
                       {
```
```
                          "meaName":"measure1",
```
```
                          "key":"temperature",
```
```
                          "unity":"C°",
```
```
                          "min":"20",
```
```
                          "max":"100",
```
```
                          "behavior":"increasing-exponential",
```
```
                          "variance":"3",
```
```
                          "prob":"5",
```
```
                          "source":"",
```
```
                          "destination":"",
```
```
                          "selArray":"M"
```
```
                       },
```
```
                       {
```
```
                          "meaName":"meausure2",
```
```
                          "key":"battery_level",
```
```
                          "unity":"V",
```
```
                          "min":"3.5",
```
```
                          "max":"3.5",
```
```
                          "variance":"3",
```
```
                          "prob":"5",
```
```
                          "source":"",
```
```
                          "destination":"",
```
```
                          "selArray":"M"
```
```
                       },
```
```
                       {  
```
```
                          "meaName":"measure3",
```
```
                          "key":"Meter",
```
```
                          "unity":"m",
```
```
                          "variance":"3",
```
```
                          "prob":"5",
```
```
                          "source":"",
```
```
                          "destination":"jzp://edv#0002.0000",
```
```
                          "values":[  
```
```
                             {  
```
```
                                "val":0.7,
```
```
                                "var":0.4,
```
```
                                "prob":70
```
```
                             },
```
```
                             {  
```
```
                                "val":1.0,
```
```
                                "var":0.4,
```
```                
                                "prob":70
```
```                           
                             },
```
```                        
                             {  
```
```
                                "val":1.2,
```
```                                
                                "var":0.4,
```
```
                                "prob":70
```
```
                             },
```
```
                             {  
```
```
                                "val":0.8,
```
```
                                "var":0.4,
```
```
                                "prob":70
```
```
                             },
```
```
                             {  
```
```
                                "val":0.6,
```
```
                                "var":0.4,
```
```
                                "prob":70
```
```
                             },
```
```
                             {  
```
```
                                "val":0.5,
```
```
                                "var":0.4,
```
```
                                "prob":70
```
```
                             }
```
```
                          ],
```
```
                          "selArray":"R"
```
```
                       }
```
```   
                    ]
```
```
                 }
```
```      
              ]
```
```   
           }
```
```
        ]
```
```
     }
```
```
}
```

- Se si vuole cominciare subito la simulazione, bisogna lasciare la key "simStartDate" come "" o con data vecchia rispetto a quella corrente, come "2000-10-04T20:02:00".

- Il tempo di simulazione è riportato in "simDuration", ed è una stringa in formato "hh:mm:ss".

- Il polling time è espresso in secondi con un intero ("pollingTime").

- Se si vuole seguire un solo modello basta impostare la key "probability" di quel modello a 1. Altrimenti, se si vogliono utilizzare più modelli, bisogna fare in modo che la somma delle probabilità raggiunga 1.

- Dentro un modello ci possono essere più misurazioni. Nel JSON object della misura vengono considerate in ordine di priorità:
1. Misure predefinite, che sono contenute nell'array "values". 

2. La seconda priorità viene data dalla presenza della key "behavior", che sta a indicare il fatto che i valori della misura nel corso del tempo seguiranno un certo andamento. I valori di "behavior" possono essere "increasing-linear", "decreasing-linear", "increasing-exponential", "decreasing-exponential", "sinusoidal", "cosinusoidal", "gaussian". 

3. Se non ci sono le keys "values" e "behavior", allora il valore viene calcolato in modo randomico dal min e max senza un criterio.

b)- API per riprendere una simulazione o far partire prima della data prefissata
```
GET "/api/v1.0/simulator/simulations/:id/play"
```
c)- API per stoppare una simulazione
```
GET "/api/v1.0/simulator/simulations/:id/stop"
```
d)- API per mettere in pausa la simulazione
```
GET "/api/v1.0/simulator/simulations/:id/pause"
```
e)- API per eliminare un'istanza di simulazione (di conseguenza non sarà più possibile vedere i risulati)
```
DELETE "/api/v1.0/simulator/simulations/:id"
```
f)- API per visualizzare l'elenco delle simulazioni con i loro stati
```
GET "/api/v1.0/simulator/simulations"
```
g)- API per visualizzare lo stato di una simulazione
```
GET "/api/v1.0/simulator/simulations/:id"
```
h)- API per visualizzare gli ultimi snapshots prodotti da una simulazine (gli ultimi 10)
```
GET "/api/v1.0/simulator/simulations/:id/snapshots"
```