# Maggiordomo
## Come avviare il bot
Questo bot è nato come un piccolo progetto personale per imparare a creare bot discord usando la libreria [JDA](https://jda.wiki).
Se volete provare prima il bot, potete trovare una versione del bot sempre online sul server discord [.gg/dorado](https://discord.gg/dorado).

In questo progetto i dati delle stanze vocali vengono salvati su un database MongoDB e la mia "libreria" [MongoStorage](https://github.com/StarlessDev/MongoStorage) gestisce la comunicazione con il database.
Quindi gli unici requisiti del bot sono:
- Java 17
- MongoDB

Potete scaricare il .jar dalle releases, se non sapete dove sono [clicca qui](github.com/StarlessDev/Maggiordomo/releases/latest) per scaricare la versione più recente. 

Tutte le build sono compilate con i JDK Eclipse Temurin, scaricabili su [adoptium.net](https://adoptium.net), per la precisione la build è `17.0.5_8`.
Per avviare il jar basta usare il solito comando `java -jar Maggiordomo-<version>-all.jar`

---
## Documentazione
Volete sapere di più sulle funzioni del bot, il modo in cui è stato pensato e istruzioni più dettagliate sul setup?

Consultate la documentazione qui sotto:

  * [Setup](https://github.com/StarlessDev/Maggiordomo/blob/main/docs/setup.md)
  * [Configurazione](https://github.com/StarlessDev/Maggiordomo/blob/main/docs/config.md)
  * [Design](https://github.com/StarlessDev/Maggiordomo/blob/main/docs/design.md), cioè come funziona il bot e le sue particolarità
  * [Interfaccia](https://github.com/StarlessDev/Maggiordomo/blob/main/docs/interface.md)
  * [Other](https://github.com/StarlessDev/Maggiordomo/blob/main/docs/other.md), una sezione che verrà riempita con informazioni che non appartengono alle categorie precedenti. Al momento ospita solamente una lista di comandi poco usati.
