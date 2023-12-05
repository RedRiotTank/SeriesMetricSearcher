package indexsearcher;

import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

public class IndexSearch {
    private static final SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yy");
    private final IndexReader reader;
    private final IndexSearcher searcher;

    private final Vector<QueryData> querieListEpisode = new Vector<>();
    private final Vector<QueryData> querieListDialog = new Vector<>();
    private Query queryEpisode = null;
    private Query queryDialog = null;

    private int maxResults = 100;

    public IndexSearch(String indexDir) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
        System.out.println("IndexSearcher created");
    }

    public void addQuery(String queryString, SearchOption so, BooleanClause.Occur occur) throws IOException, ParseException, java.text.ParseException {
        String[] query = {queryString, ""};
        this.addQuery(query, so, false, false, occur);
    }

    // si retorna null es fallo, si retorna arraylist vacio es que no hay resultados.
    public void addQuery(String[] queryString, SearchOption so, boolean includelower, boolean includeupper, BooleanClause.Occur occur) throws IOException, ParseException, java.text.ParseException {
        QueryParser parser = new QueryParser(so.getField(), so.getAnalyzer());


        String from = queryString[0].replaceAll(" ", "").replaceAll("[^0-9.]", "");
        String to = queryString[1].replaceAll(" ", "").replaceAll("[^0-9.]", "");


        switch (so.getField()){

            case "episode_number":
            case "season":
                int fromint = Integer.parseInt(from),
                    toint = Integer.parseInt(to);

                if(!includelower) fromint++;
                if(!includeupper) toint--;

                querieListEpisode.add(new QueryData(IntPoint.newRangeQuery(so.getField(),fromint, toint),occur));
                break;

            case "imdb_rating":
                float fromfloat = Float.parseFloat(from),
                        tofloat = Float.parseFloat(to);

                if(!includelower) fromfloat+=0.1;
                if(!includeupper) tofloat-=0.1;

                querieListEpisode.add(new QueryData(FloatPoint.newRangeQuery(so.getField(),fromfloat, tofloat), occur));
                break;

            case "release_date":
                String fromdateString = queryString[0].replaceAll(" ", "").replaceAll("/", "-");
                String todateString = queryString[1].replaceAll(" ", "");

                Date fromdate = new SimpleDateFormat("dd-MM-yy").parse(fromdateString),
                    todate = new SimpleDateFormat("dd-MM-yy").parse(todateString);

                if(!includelower) fromdate.setTime(fromdate.getTime() + 86400000);
                if(!includeupper) todate.setTime(todate.getTime() - 86400000);

                long fromlong = fromdate.getTime(),
                        tolong = todate.getTime();

                querieListEpisode.add(new QueryData(LongPoint.newRangeQuery(so.getField(),fromlong, tolong), occur));
                break;

            case "spoken_words":
            case "characters_list":
                querieListEpisode.add(new QueryData(parser.parse(queryString[0]), occur));
                break;
            case "spoken_words_dialog":
            case "character":
            case "location":
                querieListDialog.add(new QueryData(parser.parse(queryString[0]), occur));
                break;

            default:
                querieListEpisode.add(new QueryData(parser.parse(queryString[0]), occur));
                break;
        }
    }

    public ArrayList<MetricDoc> search() throws IOException, ParseException, java.text.ParseException {

        BooleanQuery.Builder queryBuilderEpisode = new BooleanQuery.Builder();
        BooleanQuery.Builder queryBuilderDialog = new BooleanQuery.Builder();

        boolean qEpisode = this.querieListEpisode.size() > 0;
        boolean qDialog = this.querieListDialog.size() > 0;

        for (QueryData queryData : this.querieListEpisode)
            queryBuilderEpisode.add(queryData.getQuery(), queryData.getOccur());

        for (QueryData queryData : this.querieListDialog)
            queryBuilderDialog.add(queryData.getQuery(), queryData.getOccur());

        queryEpisode = queryBuilderEpisode.build();
        queryDialog = queryBuilderDialog.build();

        if(queryEpisode == null && queryDialog == null) return null;

        TopDocs topDocsEpisode = searcher.search(queryEpisode, maxResults);
        TopDocs topDocsDialog = searcher.search(queryDialog, maxResults);

        ArrayList<Document> topDocs = new ArrayList<>();


        if(qEpisode && !qDialog)
            for(ScoreDoc episodedoc : topDocsEpisode.scoreDocs)
                topDocs.add(searcher.doc(episodedoc.doc));

        if(!qEpisode && qDialog)
            for(ScoreDoc dialogdoc : topDocsDialog.scoreDocs)
                topDocs.add(searcher.doc(dialogdoc.doc));

        if(qEpisode && qDialog)
            for(ScoreDoc dialogdoc : topDocsDialog.scoreDocs) {
                for(ScoreDoc episodedoc : topDocsEpisode.scoreDocs) {
                    Document ddoc = searcher.doc(dialogdoc.doc);
                    Document edoc = searcher.doc(episodedoc.doc);

                    if(ddoc.get("episode_number_stored").equals(edoc.get("episode_number_stored"))) {
                        topDocs.add(searcher.doc(dialogdoc.doc));
                    }
                }
            }

        this.queryEpisode = null;
        this.querieListEpisode.clear();
        this.querieListDialog.clear();

        return generateMetricDocList(topDocs);
    }

    public Document getEpisodeDoc(String episode_number) throws IOException, ParseException, java.text.ParseException {

        addQuery("episode", SearchOption.DOC_TYPE, BooleanClause.Occur.MUST);

        String[] bounds = {episode_number, episode_number};
        addQuery(bounds, SearchOption.EPISODE_NUMBER, true, true, BooleanClause.Occur.MUST);

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        for (QueryData queryData : this.querieListEpisode)
            queryBuilder.add(queryData.getQuery(), queryData.getOccur());

        queryEpisode = queryBuilder.build();

        if(queryEpisode == null) return null;

        TopDocs topDocs = searcher.search(queryEpisode, maxResults);
        this.queryEpisode = null;
        this.querieListEpisode.clear();

        return searcher.doc(topDocs.scoreDocs[0].doc);
    }

    public ArrayList<MetricDoc> allFieldsSearch(String queryString) throws IOException, ParseException, java.text.ParseException {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();


        for (SearchOption so : SearchOption.values()) {
            QueryParser parser = new QueryParser(so.getField(), so.getAnalyzer());
            Query tquery = parser.parse(queryString);
            queryBuilder.add(tquery, BooleanClause.Occur.SHOULD);
        }
        Query allq = queryBuilder.build();

        TopDocs topDocs = searcher.search(allq, maxResults);

        //return generateMetricDocList(topDocs);
        return null;

    }

    private ArrayList<MetricDoc> generateMetricDocList( ArrayList<Document> topDocs) throws IOException, ParseException, java.text.ParseException {
        ArrayList<MetricDoc> metricDocList = new ArrayList<>();

        for (Document doc : topDocs)
            metricDocList.add(new MetricDoc(doc));

        return metricDocList;
    }

    public void closeIndex() throws IOException {
        reader.close();
    }
}
