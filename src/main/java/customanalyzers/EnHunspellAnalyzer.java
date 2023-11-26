package customanalyzers;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.ParseException;

public class EnHunspellAnalyzer extends Analyzer {


    //final Tokenizer source = new StandardTokenizer();

    Dictionary dictionary;

    public EnHunspellAnalyzer() throws IOException, ParseException {
        InputStream affixStream = getClass().getResourceAsStream("/en_US.aff"),
                dictStream = getClass().getResourceAsStream("/en_US.dic");
        FSDirectory directoryTemp = null;

        if(System.getProperty("os.name").toLowerCase().contains("nix") ||System.getProperty("os.name").toLowerCase().contains("nux")){
            directoryTemp = FSDirectory.open(Paths.get("/tmp"));
        }
        else if(System.getProperty("os.name").toLowerCase().contains("win")){
            directoryTemp = FSDirectory.open(Paths.get("/temp"));
        }

        if(affixStream != null && directoryTemp != null)
            dictionary = new Dictionary(directoryTemp, "temporalFile", affixStream, dictStream);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();

        TokenFilter lowerCaseFilter = new LowerCaseFilter(source);
        TokenFilter stopFilter = new StopFilter(lowerCaseFilter, EnglishAnalyzer.getDefaultStopSet());
        TokenStream result = new PorterStemFilter(stopFilter);

        //TokenStream result = new HunspellStemFilter(source, dictionary, true, true);


        return new TokenStreamComponents(source, result);
    }

}
