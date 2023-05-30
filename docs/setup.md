## Setup
---
### Definizioni
Prima di iniziare con il setup, bisogna comprendere i vari ruoli di cui il bot ha bisogno per funzionare:
  
  + **Public role**: è il ruolo che **tutti** gli utenti che hanno accesso al bot devono avere: ad esempio può essere `@everyone`, il ruolo che viene aggiunto all'utente dopo aver completato una verifica (`@Utente`), un ruolo legato al sistema di livelli (`@Livello 30`), etc... 
  + **Premium roles**: questa è una lista di ruoli che ha la possibilità di usare le stanze fissate. Gli utenti che hanno il permesso **amministratore** vengono considerati idonei ai privilegi degli utenti premium, senza bisogno di aggiungere altri ruoli alla lista.
  + **Banned roles**: questa è una lista di ruoli a cui viene negato l'accesso alle vocali create dal bot (sia temporanee che fissate) e a cui è impossibile usare le funzioni del bot.

### Istruzioni
Il primo comando da eseguire è `/maggiordomo setup <role>`.

Questo comando accetta come parametro un ruolo (al di sotto del ruolo del bot) che diventerà il **public role** del server che il bot userà. Eseguito il comando, verrà creata in automatico la categoria, il canale testuale per l'interfaccia e il canale vocale per creare la stanza.

![](https://i.imgur.com/uAjzG0L.png)

Teoricamente, il bot **è già pronto all'uso**. Ora è possibile aggiungere ruoli alla lista **premium** o alla lista dei **bannati** con i seguenti comandi:

  + `/maggiordomo premium <role>`
  + `/maggiordomo banned <role>`

Per rimuovere un ruolo da una delle due liste basta eseguire nuovamente lo stesso comando con cui avete aggiunto il ruolo.
