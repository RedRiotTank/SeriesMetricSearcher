package indexsearcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public enum SearchOption {

    SPOKEN_WORDS("spoken_words",null),
    CHARACTER("character",new StandardAnalyzer()),
    LOCATION("location",null),
    TITLE("title",null),
    ;


    private final String field;
    final Analyzer analyzer;
    SearchOption(String option, Analyzer analyzer) {
        this.field = option;
        this.analyzer = analyzer;
    }

    public String getField() {
        return field;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }
}
