package org.htt;

import indexsearcher.IndexSearch;
import indexsearcher.MetricDoc;
import indexsearcher.SearchOption;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import ui.IndexSearchUserInterface;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException, ParseException, java.text.ParseException {

       IndexSearch indexSearch = new IndexSearch("E:\\dev\\SeriesMetricSearcher\\files\\index");

       indexSearch.addQuery("halloween", SearchOption.SPOKEN_WORDS_DIALOG, BooleanClause.Occur.MUST);
       ArrayList<MetricDoc> ff = indexSearch.search();

       //Ejemplo Nº de episodio
       String[] bounds = {"49", "50"};
       indexSearch.addQuery(bounds, SearchOption.EPISODE_NUMBER, false, true, BooleanClause.Occur.MUST);
       ArrayList<MetricDoc> s = indexSearch.search();        //MUST = AND, SHOULD = OR, MUST_NOT = NOT

        String[] boundss = {"19", "21"};
        indexSearch.addQuery(boundss, SearchOption.SEASON, true, true, BooleanClause.Occur.MUST);
        ArrayList<MetricDoc> h = indexSearch.search();        //MUST = AND, SHOULD = OR, MUST_NOT = NOT

       //spoken words
        indexSearch.addQuery("halloween", SearchOption.SPOKEN_WORDS, BooleanClause.Occur.MUST);
        ArrayList<MetricDoc> f = indexSearch.search();


       //Ejemplo character:
        indexSearch.addQuery("mariom", SearchOption.CHARACTER, BooleanClause.Occur.MUST);
        ArrayList<MetricDoc> b = indexSearch.search();

        //Localizacion
        indexSearch.addQuery("school", SearchOption.LOCATION, BooleanClause.Occur.MUST);
        ArrayList<MetricDoc> c = indexSearch.search();

        //titulo
        indexSearch.addQuery("homer", SearchOption.TITLE, BooleanClause.Occur.MUST);
        ArrayList<MetricDoc> d = indexSearch.search();


        //fecha
        String[] bounds2 = {"3/11-02", "10-11-02"};     //formatos aceptados: dd-mm-yy dd/mm/yy d/m/yy
        indexSearch.addQuery(bounds2, SearchOption.RELEASE_DATE, false, true, BooleanClause.Occur.MUST);
        ArrayList<MetricDoc> e = indexSearch.search();        //MUST = AND, SHOULD = OR, MUST_NOT = NOT

        //imdb_rating
        String[] bounds3 = {"8.5", "8.5"};
        indexSearch.addQuery(bounds3, SearchOption.IMDB_RATING, true, true, BooleanClause.Occur.MUST);
        ArrayList<MetricDoc> g = indexSearch.search();


        // EJEMPLO MULTIQUERY:

        String[] b1 = {"7.2", "8.5"};     //formatos aceptados: dd-mm-yy dd/mm/yy d/m/yy
        indexSearch.addQuery(b1, SearchOption.IMDB_RATING, true, true, BooleanClause.Occur.MUST);

        indexSearch.addQuery("halloween", SearchOption.SPOKEN_WORDS, BooleanClause.Occur.MUST);

        ArrayList<MetricDoc> multiq = indexSearch.search();


        //ejemplo global query
        ArrayList<MetricDoc> all = indexSearch.allFieldsSearch("halloween 777");


        String[] b6 = {"50", "400"};     //formatos aceptados: dd-mm-yy dd/mm/yy d/m/yy
        indexSearch.addQuery(b6, SearchOption.EPISODE_NUMBER, true, true, BooleanClause.Occur.MUST);

        indexSearch.addQuery("homer", SearchOption.CHARACTER, BooleanClause.Occur.MUST);

        ArrayList<MetricDoc> quer = indexSearch.search();

        indexSearch.closeIndex();

    }
}