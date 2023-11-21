package indexsearcher;

import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexSearch {
    private static final SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yy");
    private final IndexReader reader;
    private final IndexSearcher searcher;

    private final Vector<Query> querieList = new Vector<>();
    private Query q = null;

    private int maxResults = 10;

    public IndexSearch(String indexDir) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
        System.out.println("IndexSearcher created");
    }

    public void addQuery(String queryString, SearchOption so) throws IOException, ParseException, java.text.ParseException {
        String[] query = {queryString};
        this.addQuery(query, so, false, false);
    }


    private void addQueryNumber(String[] queryString, SearchOption so, boolean includelower, boolean includeupper){
        String from = queryString[0].replaceAll(" ", "");
        String to = queryString[1].replaceAll(" ", "");

        if(so.getField().equals("imdb_rating")) {

            if(from.contains(".")){
                from = from.replaceAll("\\.", "");
            } else {
                from = from + "0";
            }

            if(to.contains(".")){
                to = to.replaceAll("\\.", "");
            } else {
                to = to + "0";
            }
        }

        querieList.add(TermRangeQuery.newStringRange(so.getField(),from,to,includelower,includeupper));
    }

    private void addQueryDate(String[] queryString, SearchOption so, boolean includelower, boolean includeupper) throws java.text.ParseException {
        String from = queryString[0].replaceAll(" ", "").replaceAll("/", "-");
        String to = queryString[1].replaceAll(" ", "");

        from = setZeros(from);
        to = setZeros(to);

        if(from.length() != 8 || to.length() != 8) querieList.add(null);


        String fromstring = Long.toString(dateformat.parse(from).getTime());
        String tostring = Long.toString(dateformat.parse(to).getTime());

        querieList.add(TermRangeQuery.newStringRange(so.getField(),fromstring,tostring,includelower,includeupper));
    }



    // si retorna null es fallo, si retorna arraylist vacio es que no hay resultados.
    public void addQuery(String[] queryString, SearchOption so, boolean includelower, boolean includeupper) throws IOException, ParseException, java.text.ParseException {
        QueryParser parser = new QueryParser(so.getField(), so.getAnalyzer());
        Query query = null;
        switch (so.getField()){

            case "imdb_rating":
            case "episode_number":
            case "season":
                addQueryNumber(queryString, so, includelower, includeupper);

                break;
            case "release_date":
                addQueryDate(queryString, so, includelower, includeupper);
                break;

            default:
                querieList.add(parser.parse(queryString[0]));
                break;

        }


    }

    private void booleanQuery( BooleanClause.Occur occur) {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        for (Query query : this.querieList) {
            queryBuilder.add(query, occur);
        }
        q = queryBuilder.build();
    }

    public ArrayList<MetricDoc> search(BooleanClause.Occur occur) throws IOException {
        booleanQuery(occur);
        if(q == null) return null;
        TopDocs topDocs = searcher.search(q, maxResults);
        this.q = null;
        this.querieList.clear();

        return generateMetricDocList(topDocs);
    }

    private String setZeros(String date){
        String[] dateparts = date.split("-");

       for(int i=0; i<3; i++){
           if(dateparts[i].length() == 1){
               dateparts[i] = "0" + dateparts[i];
           }
       }


        return dateparts[0] + "-" + dateparts[1] + "-" + dateparts[2];
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
