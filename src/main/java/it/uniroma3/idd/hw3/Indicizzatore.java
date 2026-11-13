package it.uniroma3.idd.hw3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Indicizza tutti i file .txt di una cartella.
 * Uso: Indicizzatore [cartella dei .txt] [cartella dell'indice]
 */
public class Indicizzatore {

    public static void main(String[] args) throws IOException {
        Path cartellaTxt = Paths.get(args.length > 0 ? args[0] : "dataset");
        Path cartellaIndice = Paths.get(args.length > 1 ? args[1] : "index");

        List<Path> files;
        try (Stream<Path> s = Files.list(cartellaTxt)) {
            files = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
        }

        long inizio = System.currentTimeMillis();

        Directory directory = FSDirectory.open(cartellaIndice);
        IndexWriterConfig config = new IndexWriterConfig(Analizzatori.perCampo());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // riparte da zero ogni volta
        IndexWriter writer = new IndexWriter(directory, config);

        for (Path file : files) {
            Document doc = new Document();
            doc.add(new TextField(Analizzatori.NOME, file.getFileName().toString(), Field.Store.YES));
            doc.add(new TextField(Analizzatori.CONTENUTO, Files.readString(file), Field.Store.NO));
            doc.add(new StoredField("percorso", file.toAbsolutePath().toString()));
            writer.addDocument(doc);
        }

        writer.commit();
        writer.close();
        directory.close();

        long durata = System.currentTimeMillis() - inizio;
        System.out.println("File indicizzati: " + files.size());
        System.out.println("Tempo di indicizzazione: " + durata + " ms");
        System.out.println("Indice salvato in: " + cartellaIndice.toAbsolutePath());
    }
}
