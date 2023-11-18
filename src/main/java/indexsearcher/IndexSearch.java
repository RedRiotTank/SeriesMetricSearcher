package indexsearcher;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class IndexSearch {
    private IndexReader reader;
    private IndexSearcher searcher;

    private int maxResults = 10;

    public IndexSearch(String indexDir) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
        System.out.println("IndexSearcher created");
    }

    public ArrayList<MetricDoc> search(String queryString, SearchOption so) throws IOException, ParseException {
        QueryParser parser = new QueryParser(so.getField(), so.getAnalyzer());
        Query query = parser.parse(queryString);

        TopDocs topDocs = searcher.search(query, maxResults);

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
