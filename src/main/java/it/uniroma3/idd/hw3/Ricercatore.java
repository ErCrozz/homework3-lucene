package it.uniroma3.idd.hw3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Legge le query da console, interroga l'indice e stampa i risultati.
 *
 * Sintassi: la query inizia con "nome" o "contenuto", seguita dai termini.
 *   nome xpath
 *   contenuto record linkage
 *   contenuto "inverted index"        (phrase query)
 *   contenuto +wrapper +"web site"    (+ = termine obbligatorio)
 * Riga vuota o "esci" per terminare.
 *
 * Uso: Ricercatore [cartella dell'indice]
 */
public class Ricercatore {

    private static final int MAX_RISULTATI = 10;

    public static void main(String[] args) throws IOException {
        String cartellaIndice = args.length > 0 ? args[0] : "index";

        Directory directory = FSDirectory.open(Paths.get(cartellaIndice));
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader); // ranking di default: BM25
        System.out.println("Indice aperto: " + reader.numDocs() + " documenti.");

        QueryParser parserNome = new QueryParser(Analizzatori.NOME, Analizzatori.perNome());
        QueryParser parserContenuto = new QueryParser(Analizzatori.CONTENUTO, Analizzatori.perContenuto());

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("\nquery> ");
            String riga = console.readLine();
            if (riga == null || riga.isBlank() || riga.trim().equalsIgnoreCase("esci")) {
                break;
            }

            // separo la parola chiave (il campo) dai termini
            String[] parti = riga.trim().split("\\s+", 2);
            if (parti.length < 2) {
                System.out.println("Scrivi: nome <termini>  oppure  contenuto <termini>");
                continue;
            }
            QueryParser parser;
            switch (parti[0].toLowerCase()) {
                case "nome" -> parser = parserNome;
                case "contenuto" -> parser = parserContenuto;
                default -> {
                    System.out.println("Campo sconosciuto '" + parti[0] + "': usa nome o contenuto");
                    continue;
                }
            }

            try {
                Query query = parser.parse(parti[1]);
                cerca(searcher, query);
            } catch (ParseException e) {
                System.out.println("Query non valida: " + e.getMessage());
            }
        }

        reader.close();
        directory.close();
    }

    private static void cerca(IndexSearcher searcher, Query query) throws IOException {
        long inizio = System.nanoTime();
        TopDocs hits = searcher.search(query, MAX_RISULTATI);
        double ms = (System.nanoTime() - inizio) / 1_000_000.0;

        System.out.println("Query Lucene: " + query);
        System.out.printf("Trovati %d documenti (%.1f ms), primi %d:%n",
                hits.totalHits.value(), ms, hits.scoreDocs.length);

        StoredFields storedFields = searcher.storedFields();
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = hits.scoreDocs[i];
            Document doc = storedFields.document(scoreDoc.doc);
            System.out.printf("%2d. [%.3f] %s%n", i + 1, scoreDoc.score, doc.get(Analizzatori.NOME));
        }
    }
}
