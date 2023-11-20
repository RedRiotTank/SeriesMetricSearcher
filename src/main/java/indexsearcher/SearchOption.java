package indexsearcher;

import customanalyzers.EnHunspellAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.IOException;
import java.text.ParseException;

public enum SearchOption {
    EPISODE_NUMBER("episode_number"),
    SPOKEN_WORDS("spoken_words"),
    SPOKEN_WORDS_DIALOG("spoken_wordsDialog"),
    CHARACTER("character"),
    CHARACTER_LIST("characters_list"),
    LOCATION("location"),
    IMDB_RATING("imdb_rating"),
    IMDB_VOTES("imdb_votes"),
    RELEASE_DATE("release_date"),
    SEASON("season"),
    TITLE("title"),
    EPISODE_VIEWS("episode_views"),
    ;

    Analyzer
            whitespaceAnalyzer,
            standardAnalyzer,
            enHunspellAnalyzer;


    private final String field;
    SearchOption(String option){
        this.field = option;

        whitespaceAnalyzer = new WhitespaceAnalyzer();
        standardAnalyzer = new StandardAnalyzer();

        try{
            enHunspellAnalyzer = new EnHunspellAnalyzer();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

    }

    public String getField() {
        return field;
    }

    public Analyzer getAnalyzer() {

        switch (field){
            case "imdb_rating":
            case "imdb_votes":
            case "release_date":
            case "season":
            case "episode_views":
                return whitespaceAnalyzer;

            case "episode_number":
            case "character":
            case "character_list":
            case "location":
                return standardAnalyzer;

            case "spoken_words":
            case "spoken_wordsDialog":
            case "title":
                return enHunspellAnalyzer;

                default:
                return standardAnalyzer;
        }
    }
}
