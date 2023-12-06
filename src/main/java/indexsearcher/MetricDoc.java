package indexsearcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MetricDoc {

    private String episode_number = null;
    private String spoken_words = null;
    private String spoken_words_dialog = null;

    private String character = null;
    private String character_list = null;
    private String location = null;
    private String imdb_rating = null;

    private String release_date = null;

    private String season = null;
    private String title = null;
    private String views = null;

    IndexSearch indexSearch = IndexSearch.getInstance(GlobalVals.getRutaIndex());

    public MetricDoc(Document doc) throws IOException, ParseException, java.text.ParseException {

        this.episode_number = doc.get("episode_number_stored");

        String doc_type = doc.get("doc_type");

        if(doc_type.equals("episode")){

            this.spoken_words = doc.get("spoken_words");

            this.imdb_rating = doc.get("imdb_rating_stored");

            if(imdb_rating != null)
                this.release_date = new SimpleDateFormat("dd-MM-yy").format((new Date(Long.parseLong(doc.get("release_date_stored")))));
            else
                this.release_date = null;
            this.season = doc.get("season_stored");

            this.title = doc.get("title");
            this.views = doc.get("episode_views");

            this.character_list = doc.get("characters_list");

            this.character = "-";
            this.location = "-";
            this.spoken_words_dialog = "-";


        } else if(doc_type.equals("dialog")){

            Document episodedoc = indexSearch.getEpisodeDoc(this.episode_number);

            if(doc.get("character") != null)
                this.character = doc.get("character");

            if(doc.get("location") != null)
                this.location = doc.get("location");
             else
                 this.location = "";

            this.spoken_words_dialog = doc.get("spoken_words_dialog");

            this.spoken_words = episodedoc.get("spoken_words");

            this.imdb_rating = episodedoc.get("imdb_rating_stored");

            if(imdb_rating != null)
                this.release_date = new SimpleDateFormat("dd-MM-yy").format((new Date(Long.parseLong(episodedoc.get("release_date_stored")))));
            else
                this.release_date = null;
            this.season = episodedoc.get("season_stored");

            this.title = episodedoc.get("title");
            this.views = episodedoc.get("episode_views");
            this.character_list = episodedoc.get("characters_list");
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

    public String getRelease_date() {
        return release_date;
    }

    public String getSeason() {
        return season;
    }

    public String getTitle() {
        return title;
    }

    public String getSpoken_words_dialog() {
        return spoken_words_dialog;
    }

    public String getViews() {
        return views;
    }
}
