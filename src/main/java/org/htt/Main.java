package org.htt;

import indexsearcher.IndexSearch;
import indexsearcher.MetricDoc;
import indexsearcher.SearchOption;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException, ParseException {

        IndexSearch indexSearch = new IndexSearch("E:\\dev\\SeriesMetricSearcher\\files\\index");
        ArrayList<MetricDoc> list =  indexSearch.search("mariom", SearchOption.CHARACTER);
        indexSearch.closeIndex();

    }
}