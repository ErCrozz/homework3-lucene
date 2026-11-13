# Homework 3 – Ingegneria dei Dati 2026/2027

Indicizzazione e ricerca di file `.txt` con Apache Lucene 10.3.2 (Java 21, Maven).

## Struttura

- `src/main/java/it/uniroma3/idd/hw3/Analizzatori.java`: gli analyzer dei due campi (`nome`, `contenuto`)
- `src/main/java/it/uniroma3/idd/hw3/Indicizzatore.java`: indicizza i `.txt` di una cartella
- `src/main/java/it/uniroma3/idd/hw3/Ricercatore.java`: legge le query da console e stampa i risultati
- `src/main/java/it/uniroma3/idd/hw3/Valutatore.java`: precision, recall e F1 con le query e i giudizi di rilevanza CACM
- `dataset/`: 3204 file `.txt`, uno per articolo della collezione CACM (Communications of the ACM, 1958–1979)
- `valutazione/`: `query.text` e `qrels.text` della collezione CACM
- `query-di-test.txt`: le query usate per il test manuale

Fonte del dataset: [CACM test collection, University of Glasgow](http://ir.dcs.gla.ac.uk/resources/test_collections/cacm/).
Il file originale `cacm.all` è stato diviso in un file per articolo (titolo, abstract, autori).

## Uso

```
mvn compile

# indicizza (default: cartella dataset, indice in index/)
mvn exec:java -Dexec.mainClass=it.uniroma3.idd.hw3.Indicizzatore -Dexec.args="dataset index"

# ricerca interattiva
mvn exec:java -Dexec.mainClass=it.uniroma3.idd.hw3.Ricercatore -Dexec.args="index"

# oppure con le query di test
mvn exec:java -Dexec.mainClass=it.uniroma3.idd.hw3.Ricercatore < query-di-test.txt

# precision / recall / F1
mvn exec:java -Dexec.mainClass=it.uniroma3.idd.hw3.Valutatore -Dexec.args="index valutazione"
```

## Sintassi delle query

La query inizia con il campo (`nome` o `contenuto`), seguito dai termini:

```
nome compiler
contenuto garbage collection            (OR dei termini)
contenuto "garbage collection"          (phrase query)
contenuto +deadlock +"operating system" (+ obbligatorio, - escluso)
```

Riga vuota o `esci` per uscire.
