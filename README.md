# Maggiordomo
Un bot creato per gestire e personalizzare le stanze degli utenti in modo completamente gratuito.

### Requisiti
In questo progetto i dati delle stanze vocali vengono salvati su un database MongoDB e la mia "libreria" [MongoStorage](https://github.com/StarlessDev/MongoStorage) gestisce la comunicazione con il database.
Quindi gli unici requisiti del bot sono:
- Java 17
- MongoDB

### Setup
É disponibile sul server [.gg/dorado](https://discord.gg/dorado) una versione del bot che potete provare, ma non potrete aggiungerlo al vostro server.
Gli hosting non crescono sugli alberi e questo bot è stato concepito per essere completamente gratis, quindi non è possibile (per me almeno) hostare il bot.

L'unica opzione disponibile è hostarlo voi stessi, usando [la guida](https://github.com/StarlessDev/Maggiordomo/blob/docs/creation.md) che vi aiuterà a mettere online il vostro bot: partendo dalla creazione del bot su discord, fino al setup vero e proprio sul server discord.

### Funzioni
In termini di personalizzazione della stanza il bot rende possibile cambiare:
- Nome
- Capienza
- Gli utenti che possono accedere liberamente ad essa, detti: *fidati*
- Gli utenti banditi dalla stanza, detti *bannati*
- Impostazioni della privacy: puoi scegliere se rendere la stanza accessible a tutti oppure solamente agli utenti fidati.

> Tutte queste caratteristiche possono essere modificate e salvate anche quando la stanza non è fisicamente presente nella categoria.

Lo sviluppo del bot continua! Al momento stiamo lavorando per aggiungere:
- [ ] Supporto per più di una categoria dedicata alle stanze
- [ ] Personalizzazione del pannello di controllo (titolo e descrizione dell'embed)

### La particolarità di questo bot
Oltre alle stanze temporanee, che si cancellano quando non ha utenti che la usano, Maggiordomo offre anche le *stanze fissate*: cioè stanze che non si cancellano e rimangono disponibili 24 ore su 24.

Le stanze sono sempre ordinate in base alla loro tipologia: le stanze temporanee sono **sempre in cima**, mentre le stanze fissate sono sempre **in fondo** alla categoria.

![Separazione stanze](https://i.imgur.com/Zrz1eYQ.jpg)

> *Ma se ci sono troppe stanze fissate senza utenti dentro, il discord non sembrerà vuoto e inattivo?*

Questo è quello che ho pensato mentre facevo il bot e ho trovato una soluzione: solamente le stanze con utenti al suo interno sono visibili al pubblico, cioè a tutti quelli che hanno il *Public role* (controlla la documentazione se non sai cosa sia).

![Confronto della vista admin e utente](https://i.imgur.com/4z9hIFV.jpeg)

