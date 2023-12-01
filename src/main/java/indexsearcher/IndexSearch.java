package indexsearcher;

import customanalyzers.EnHunspellAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexSearch {
    private static final SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yy");
    private final IndexReader reader;
    private final IndexSearcher searcher;

    private final Vector<QueryData> querieList = new Vector<>();
    private Query q = null;

    private int maxResults = 50;

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

                querieList.add(new QueryData(IntPoint.newRangeQuery(so.getField(),fromint, toint),occur));
                break;

            case "imdb_rating":
                float fromfloat = Float.parseFloat(from),
                        tofloat = Float.parseFloat(to);

                if(!includelower) fromfloat+=0.1;
                if(!includeupper) tofloat-=0.1;

                querieList.add(new QueryData(FloatPoint.newRangeQuery(so.getField(),fromfloat, tofloat), occur));
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

                querieList.add(new QueryData(LongPoint.newRangeQuery(so.getField(),fromlong, tolong), occur));
                break;

            default:
                querieList.add(new QueryData(parser.parse(queryString[0]), occur));
                break;
        }
    }

    public ArrayList<MetricDoc> search() throws IOException {

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        for (QueryData queryData : this.querieList)
            queryBuilder.add(queryData.getQuery(), queryData.getOccur());

        q = queryBuilder.build();

        if(q == null) return null;

        TopDocs topDocs = searcher.search(q, maxResults);
        this.q = null;
        this.querieList.clear();

        return generateMetricDocList(topDocs);
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

        return generateMetricDocList(topDocs);

    }

    private ArrayList<MetricDoc> generateMetricDocList( TopDocs topDocs) throws IOException {
        ArrayList<MetricDoc> metricDocList = new ArrayList<>();

        for (ScoreDoc doc : topDocs.scoreDocs)
            metricDocList.add(new MetricDoc(searcher.doc(doc.doc)));

        return metricDocList;
    }

    public void setMaxResults(int maxResults){
        this.maxResults = maxResults;
    }

    public int getMaxResults(){
        return maxResults;
    }

    public void closeIndex() throws IOException {
        reader.close();
    }
}
