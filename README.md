# SyncETS
SyncETS permet de synchroniser l'emploi du temps ETS sur son Google Calendar dans le cloud.


## Configuration
L'application nécessite d'activer l'API Google calendar sur son propre compte Google. 
Il faut dans un premier temps créer un projet à l'adresse suivante : https://console.cloud.google.com/apis/dashboard

Les API à sélectionner sont les suivantes : Google Calendar API, Tasks API

Il faut ensuite créer un identifiant pour autoriser l'application à contacter les API de Google.

Si l'application est compilée avec un certificat debug, c'est celui-ci qui devra être utilisé pour générér l'empreinte du certificat de signature.
Par exemple, mon fichier debug.keystore se trouve dans ~/.android/debug.keystore.

J'exécute donc : 

```bash
keytool -exportcert -keystore ~/.android/debug.keystore -list -v
```
(le mot de passe par défaut pour un debug keystore est "android")

On copie finalement le SHA1 dans l'interface d'identifiants de Google en spécifiant le nom de package de l'application.






