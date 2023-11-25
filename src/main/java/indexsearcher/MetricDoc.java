package indexsearcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MetricDoc {

    private final String episode_number;
    private final String spoken_words;

    private final String character;
    private final String character_list;
    private final String location;
    private final String imdb_rating;
    private final String imdb_votes;

    private final String release_date;

    private final String season;
    private final String title;
    private final String views;

    public MetricDoc(Document doc){
        this.episode_number = doc.get("episode_number_stored");
        this.spoken_words = doc.get("spoken_words");

        String imdbRatingString = doc.get("imdb_rating_stored");
        //double imdbRatingDouble = Double.parseDouble(imdbRatingString);
        //String imdbRatingStringNormalized = String.valueOf(imdbRatingDouble);

        this.imdb_rating = imdbRatingString;

        this.imdb_votes = doc.get("imdb_votes");

        this.release_date = new SimpleDateFormat("dd-MM-yy").format((new Date(Long.parseLong(doc.get("release_date_stored")))));

        this.season = doc.get("season_stored");

        this.title = doc.get("title");
        this.views = doc.get("episode_views");

        if(doc.get("character") != null){
            this.character = doc.get("character");
            this.character_list = "";
        }else{
            this.character = "";
            this.character_list = doc.get("character_list");
        }

        if(doc.get("location") != null){
            this.location = doc.get("location");

        } else {
                this.location = "";
            }
    }

    public String getEpisode_number() {
        return episode_number;
    }

    public String getSpoken_words() {
        return spoken_words;
    }

    public String getCharacter() {
        return character;
    }

    public String getCharacter_list() {
        return character_list;
    }

    public String getLocation() {
        return location;
    }

    public String getImdb_rating() {
        return imdb_rating;
    }

    public String getImdb_votes() {
        return imdb_votes;
    }

    public String getRelease_date() {
        return release_date;
    }

    public String getSeason() {
        return season;
    }

    public String getTitle() {
        return title;
    }

    public String getViews() {
        return views;
    }
}
