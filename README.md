# SyncETS
SyncETS permet de synchroniser l'emploi du temps ETS sur son Google Calendar dans le cloud.

![Interface_SyncETS](https://raw.githubusercontent.com/ttauveron/SyncETS/images/interface_SyncETS.png)

## Utilisation
L'écran de connexion demande les identifiants ÉTS : code universel (AA12345) et mot de passe ETS afin d'accéder à l'API Signets et récupérer la liste des cours.
On choisit ensuite le compte Google sur lequel on veut que la synchronisation s'effectue (Google calendar).

![Google calendar with SyncETS](https://raw.githubusercontent.com/ttauveron/SyncETS/images/google_calendar_example.png)

L'application peut envoyer une notification 15 minutes avant le début de chaque cours. 
Cela permet d'avoir le numéro du local dans la notification sans avoir à farfouiller dans son téléphone.

## Configuration
L'application nécessite d'activer l'API Google calendar sur son propre compte Google. 
Il faut dans un premier temps créer un projet à l'adresse suivante : https://console.cloud.google.com/apis/dashboard

Les API à sélectionner sont les suivantes : Google Calendar API, Tasks API

Il faut ensuite créer un identifiant pour autoriser l'application à contacter les API de Google.

![Config identifiants Google](https://raw.githubusercontent.com/ttauveron/SyncETS/images/config_identifiants_google.png)

Si l'application est compilée avec un certificat debug, c'est celui-ci qui devra être utilisé pour générér l'empreinte du certificat de signature.
Par exemple, mon fichier debug.keystore se trouve dans ~/.android/debug.keystore.

J'exécute donc : 

```bash
keytool -exportcert -keystore ~/.android/debug.keystore -list -v
```
(le mot de passe par défaut pour un debug keystore est "android")

On copie finalement le SHA1 dans l'interface d'identifiants de Google en spécifiant le nom de package de l'application.






