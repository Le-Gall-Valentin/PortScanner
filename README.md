# Travail Pratique #1: TCP Port Scanner

## Auteurs
- LEGV17030400, Le Gall, Valentin

## Compatibilité

Java - 21

## Utilisation

### Prérequis

Un JDK 21 (LTS), par exemple Eclipse Temurin : <https://adoptium.net/temurin/releases/?version=21>.
Aucune autre dépendance : le programme n'utilise que la bibliothèque standard de Java.

Vérifiez les versions installées :

```
java -version
javac -version
```

Les commandes ci-dessous se lancent depuis le dossier `scanner/`, qui contient `src/` et ce README.
La compilation crée le dossier `out/`.

### Windows (cmd ou PowerShell)

```
javac --release 21 -d out --source-path src src\ca\uqac\scanner\Main.java
java -cp out ca.uqac.scanner.Main 127.0.0.1
```

### macOS et Linux

```
javac --release 21 -d out --source-path src src/ca/uqac/scanner/Main.java
java -cp out ca.uqac.scanner.Main 127.0.0.1
```

Remplacez `127.0.0.1` par la cible : une adresse IPv4, une adresse IPv6 ou un nom de domaine, locale ou distante.
Le programme teste les ports TCP 1 à 1023 et s'arrête proprement avec Ctrl+C.

### Exemples de sortie

Scan de la machine locale :

```
$ java -cp out ca.uqac.scanner.Main 127.0.0.1
Scanning 127.0.0.1...
--> Port 631/TCP is open.
--> Port 902/TCP is open.
Scan completed successfully.
```

Interruption par Ctrl+C en cours de scan (les ports ouverts trouvés avant l'arrêt restent affichés) :

```
$ java -cp out ca.uqac.scanner.Main 192.168.0.1
Scanning 192.168.0.1...
Scan is stopping...
Scan completed successfully.
```

Sans argument, ou avec plus d'un argument :

```
$ java -cp out ca.uqac.scanner.Main
Usage: java -cp out ca.uqac.scanner.Main <target ip or hostname>
```

Nom de domaine impossible à résoudre (la fin du message vient du système et dépend de l'OS et de sa langue) :

```
$ java -cp out ca.uqac.scanner.Main nope.invalid
Hostname could not be resolved: nope.invalid: Name or service not known
```

Les résultats sont écrits sur la sortie standard, l'usage et les erreurs sur la sortie d'erreur.

| Code de sortie | Signification                                                      |
|----------------|--------------------------------------------------------------------|
| 0              | Scan terminé                                                       |
| 1              | Nom d'hôte impossible à résoudre                                   |
| 2              | Mauvaise utilisation (aucun argument, plusieurs, ou argument vide) |
| 130            | Arrêt par Ctrl+C : code fixé par la JVM (128 + SIGINT)             |

## Architecture et choix de conception

```
src/ca/uqac/scanner/
├── Main.java                  assemble les composants et enregistre le hook d'arrêt
├── ScannerCli.java            vérifie l'argument, résout la cible, lance le scan, renvoie le code de sortie
├── ExitCode.java              codes de sortie 0, 1 et 2
├── ScanSession.java           cycle de vie du scan et affichage console thread-safe
├── ScanShutdownHook.java      arrêt propre sur Ctrl+C
├── core/                      moteur de scan, indépendant de la console et du réseau réel
│   ├── PortScanner.java           contrat : scanner une cible, signaler les ports ouverts
│   ├── ConcurrentPortScanner.java virtual threads + sémaphore (utilisé par le programme)
│   ├── SequentialPortScanner.java un port à la fois (implémentation de référence)
│   ├── PortProbe.java             contrat : « ce port est-il ouvert ? »
│   ├── ScanListener.java          notification des ports ouverts au fil de l'eau
│   ├── PortRange.java             plage de ports validée
│   └── ScanConfig.java            plage, timeout et concurrence maximale
└── net/                       adaptateurs réseau de java.net
    ├── TcpPortProbe.java          connexion TCP complète avec timeout
    ├── HostResolver.java          contrat de résolution d'une IP ou d'un nom
    ├── SystemHostResolver.java    résolution par le système (InetAddress)
    └── HostResolutionException.java
```

Les collaborateurs (résolution, sonde, flux de sortie, configuration) sont injectés par constructeur et
assemblés dans `Main`, qui se limite à quelques lignes. Le moteur `core` ne dépend d'aucune classe
concrète : il peut scanner n'importe quelle plage de ports, et des tests pourront lui fournir une fausse
sonde, un faux résolveur ou un flux de sortie en mémoire sans modifier le code.

### Ports réservés = ports 1 à 1023

L'énoncé demande de tester les ports réservés. Ce sont les ports « well-known » (ports système) de l'IANA
(RFC 6335), de 0 à 1023, réservés aux services standards (HTTP 80, HTTPS 443, SSH 22…) et dont l'ouverture
exige traditionnellement des privilèges sur les systèmes Unix. Le port 0 est exclu : c'est un joker qui,
lors d'un `bind`, demande au système de choisir un port libre. Aucun service ne peut donc y écouter, et
on ne peut pas s'y connecter. Le scan couvre les ports 1 à 1023.

### Sonde TCP et timeout

Chaque port est testé par une connexion TCP complète (`Socket#connect` avec timeout), fermée aussitôt par
un try-with-resources. Cette méthode ne demande aucun privilège et fonctionne sur tous les systèmes.
Toute `IOException` (connexion refusée, délai dépassé, hôte injoignable) signifie que le port n'est pas
ouvert.

Le timeout est d'une seconde. Un port fermé est refusé en un aller-retour : le timeout ne joue que pour
les ports filtrés par un pare-feu, qui ne répondent pas. Une seconde laisse une marge confortable pour
une cible distante.

### Scan multi-threadé : virtual threads et sémaphore

`ConcurrentPortScanner` lance un virtual thread (Java 21) par port : ces threads sont si légers qu'il est
inutile de les réutiliser dans un pool. Un `Semaphore` borne à 200 le nombre de connexions simultanées.
Cette borne n'est pas une limitation : ouvrir toutes les connexions d'un coup risquerait d'épuiser les
descripteurs de fichiers ou de saturer les files du réseau, et un paquet SYN perdu ferait passer un port
ouvert pour fermé. Même quand tous les ports sont filtrés, le scan dure environ 6 secondes
(1023 ports / 200 connexions, soit 6 vagues d'une seconde).

**L'ordre d'affichage des ports ouverts n'est pas garanti.** Chaque port est affiché dès qu'il est
trouvé, par le thread qui l'a testé. Le sémaphore est équitable, donc les ports sont testés à peu près
dans l'ordre croissant, mais un port qui répond plus vite peut apparaître avant un port plus petit.

`SequentialPortScanner` teste un port à la fois, dans l'ordre. Il sert d'implémentation de référence :
le programme utilise la version concurrente. La commande de compilation ci-dessus ne compile que les
classes utilisées par `Main` ; pour tout compiler, ouvrez le projet dans un IDE ou, sous macOS et Linux,
lancez `javac -d out $(find src -name "*.java")`.

### Arrêt par Ctrl+C

Ctrl+C est intercepté par un hook d'arrêt standard (`Runtime#addShutdownHook`). Sa logique vit dans
`ScanShutdownHook` et seul son enregistrement se trouve dans `Main`. Quand un scan est en cours, le hook
affiche `Scan is stopping...` et interrompt le thread du scan. L'interruption se propage aux virtual
threads, dont les connexions en cours sont abandonnées immédiatement. Le hook attend ensuite au plus
deux secondes la fin des tâches, puis s'assure que `Scan completed successfully.` est affiché. Le hook
s'exécute aussi à chaque fin normale du programme : le scan est alors terminé, et il n'affiche rien.

Le thread principal, les threads de scan et le hook écrivent en parallèle. `ScanSession` fait donc passer
chaque changement d'état (`IDLE`, `RUNNING`, `STOPPING`, `TERMINATED`) et chaque ligne affichée par un
même verrou. Quel que soit l'entrelacement des threads, chaque message n'apparaît qu'une fois, dans le bon
ordre, et aucun port n'est affiché après `Scan is stopping...`.

### Choix du langage

Le code utilise les apports de Java 21 là où ils servent : des records validés pour `PortRange` et
`ScanConfig`, les virtual threads, un `ExecutorService` fermé automatiquement par try-with-resources, et
un `switch` exhaustif sur l'état de la session. Il compile sans aucun avertissement avec `-Xlint:all`.

## Note légale

Ce programme accepte n'importe quelle cible, locale ou distante, sans aucune restriction. Il appartient à
l'utilisateur de n'effectuer ses essais que sur des machines qu'il est autorisé à scanner.
