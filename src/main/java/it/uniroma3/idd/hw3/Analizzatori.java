package it.uniroma3.idd.hw3;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * Analyzer usati per i due campi. Sono gli stessi in indicizzazione e in ricerca.
 */
public class Analizzatori {

    public static final String NOME = "nome";
    public static final String CONTENUTO = "contenuto";

    /**
     * Nome del file, es. "web-data-extraction_038_what-is-xpath.txt".
     * Si divide su spazi, trattini, underscore e punti (wordDelimiterGraph),
     * poi tutto in minuscolo. Niente stopword e niente stemming: il nome di un file
     * e' un identificatore, si cercano le parole cosi' come sono scritte.
     */
    public static Analyzer perNome() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter("wordDelimiterGraph")
                .addTokenFilter("lowercase")
                .build();
    }

    /**
     * Contenuto del file: testo libero in inglese.
     * EnglishAnalyzer = tokenizzazione standard, minuscolo, stopword inglesi
     * e stemming di Porter (retrieval e retrieve diventano entrambi "retriev").
     */
    public static Analyzer perContenuto() {
        return new EnglishAnalyzer();
    }

    /** Un analyzer diverso per ciascun campo. */
    public static Analyzer perCampo() throws IOException {
        Map<String, Analyzer> perCampo = Map.of(
                NOME, perNome(),
                CONTENUTO, perContenuto());
        return new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perCampo);
    }
}
