## Configurazione
Tutto il codice relativo al file di configurazione del bot è nel package `dev.starless.maggiordomo.config` e si appoggia sulla libreria [Configurate](https://github.com/SpongePowered/Configurate) di SpongePowered.
Il file è in formato YAML e ha pochi valori al suo interno. Qua sotto ho aggiunto una versione commentata della configurazione:
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