# Configurazione

---
L'unico modo per avviare il bot è tramite linea di comando con questo comando: `java -jar Maggiordomo-<versione>-all.jar`.

Al primo avvio il bot si spegnerà automaticamente dopo aver generato un file chiamato `config.yml` nella cartella `maggiordomo`.

Tutto il codice relativo al file di configurazione del bot è nel package `dev.starless.maggiordomo.config` e si appoggia sulla libreria [Configurate](https://github.com/SpongePowered/Configurate) di SpongePowered.
Il file è in formato YAML e ha pochi valori al suo interno. 

Qua sotto ho aggiunto una versione commentata della configurazione che bisogna compilare:
```yaml
# Contiene la versione della configurazione: viene usato
# per aggiornarne i valori.
# Quindi, NON DEVE ESSERE MODIFICATO
config_version: <string>

# Incolla qua il token del tuo bot
token: <string>

# Incolla qua l'uri completo per la
# connessione al database Mongo.
# es. mongodb://localhost:1337/
mongo: <string>
```
---
Step successivo: [Setup](https://github.com/StarlessDev/Maggiordomo/blob/main/docs/setup.md)
