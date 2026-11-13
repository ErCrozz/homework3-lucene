package it.uniroma3.idd.hw3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
 * Valuta il sistema con le query e i giudizi di rilevanza (qrels) della collezione CACM.
 * Per ogni query calcola precision, recall e F1 sui primi k risultati, poi la media.
 *
 * Uso: Valutatore [cartella dell'indice] [cartella con query.text e qrels.text]
 */
public class Valutatore {

    private static final int[] VALORI_K = {5, 10, 20};

    public static void main(String[] args) throws IOException, ParseException {
        String cartellaIndice = args.length > 0 ? args[0] : "index";
        String cartellaValutazione = args.length > 1 ? args[1] : "valutazione";

        Map<Integer, String> query = leggiQuery(cartellaValutazione + "/query.text");
        Map<Integer, Set<Integer>> rilevanti = leggiQrels(cartellaValutazione + "/qrels.text");

        Directory directory = FSDirectory.open(Paths.get(cartellaIndice));
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        StoredFields storedFields = searcher.storedFields();
        QueryParser parser = new QueryParser(Analizzatori.CONTENUTO, Analizzatori.perContenuto());

        System.out.println("Query con giudizi di rilevanza: " + rilevanti.size());
        for (int k : VALORI_K) {
            double sommaP = 0, sommaR = 0, sommaF1 = 0;

            for (Map.Entry<Integer, Set<Integer>> e : rilevanti.entrySet()) {
                // il testo delle query CACM contiene caratteri speciali per il QueryParser (?, :, -, ...)
                Query q = parser.parse(QueryParser.escape(query.get(e.getKey())));
                TopDocs hits = searcher.search(q, k);

                int trovatiRilevanti = 0;
                for (ScoreDoc sd : hits.scoreDocs) {
                    if (e.getValue().contains(idCacm(storedFields.document(sd.doc).get(Analizzatori.NOME)))) {
                        trovatiRilevanti++;
                    }
                }
                double p = (double) trovatiRilevanti / k;
                double r = (double) trovatiRilevanti / e.getValue().size();
                double f1 = (p + r == 0) ? 0 : 2 * p * r / (p + r);
                sommaP += p;
                sommaR += r;
                sommaF1 += f1;
            }

            int n = rilevanti.size();
            System.out.printf("k=%2d  Precision=%.3f  Recall=%.3f  F1=%.3f%n",
                    k, sommaP / n, sommaR / n, sommaF1 / n);
        }

        reader.close();
        directory.close();
    }

    /** "cacm-1410_interarrival-statistics....txt" -> 1410 */
    private static int idCacm(String nomeFile) {
        return Integer.parseInt(nomeFile.substring(5, 9));
    }

    /** query.text: ogni query inizia con ".I n", il testo e' nella sezione ".W". */
    private static Map<Integer, String> leggiQuery(String file) throws IOException {
        Map<Integer, String> query = new HashMap<>();
        int id = -1;
        String sezione = "";
        StringBuilder testo = new StringBuilder();
        List<String> righe = Files.readAllLines(Paths.get(file), StandardCharsets.ISO_8859_1);
        for (String riga : righe) {
            if (riga.startsWith(".I ")) {
                if (id != -1) query.put(id, testo.toString().trim());
                id = Integer.parseInt(riga.substring(3).trim());
                testo = new StringBuilder();
                sezione = "";
            } else if (riga.matches("^\\.[A-Z]$")) {
                sezione = riga;
            } else if (sezione.equals(".W")) {
                testo.append(riga).append(' ');
            }
        }
        query.put(id, testo.toString().trim());
        return query;
    }

    /** qrels.text: righe "idQuery idDocumento 0 0". */
    private static Map<Integer, Set<Integer>> leggiQrels(String file) throws IOException {
        Map<Integer, Set<Integer>> rilevanti = new TreeMap<>();
        for (String riga : Files.readAllLines(Paths.get(file))) {
            String[] parti = riga.trim().split("\\s+");
            if (parti.length < 2) continue;
            rilevanti.computeIfAbsent(Integer.parseInt(parti[0]), x -> new HashSet<>())
                    .add(Integer.parseInt(parti[1]));
        }
        return rilevanti;
    }
}
