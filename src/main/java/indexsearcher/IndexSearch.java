package indexsearcher;

import org.apache.lucene.document.*;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
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
import java.util.List;
import java.util.Vector;

public class IndexSearch {
    private static final SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yy");
    private final IndexReader reader;
    // TODO: añadir un TaxonomyReader para abrir el indice de facetas y poder trabajar con estas
    private final IndexSearcher searcher;

    private final FacetsConfig facetsConfig = new FacetsConfig();
    private FacetsCollector facetsCollector;
    private final TaxonomyReader taxoReader;

    private static IndexSearch instance;
    private final Vector<QueryData> querieListEpisode = new Vector<>();
    private final Vector<QueryData> querieListDialog = new Vector<>();
    private Query queryEpisode = null;
    private Query queryDialog = null;

    private int maxResults = 100;

    public IndexSearch(String indexDir) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
        Directory facetsDirectory = FSDirectory.open(Paths.get(indexDir).resolveSibling("facets"));
        taxoReader = new DirectoryTaxonomyReader(facetsDirectory);
        initializeFacets();
        System.out.println("IndexSearcher created");
    }

    private void initializeFacets(){
        facetsConfig.setHierarchical("Character", true);
        facetsConfig.setHierarchical("Location", true);
        facetsConfig.setIndexFieldName("ImdbRating", "imdb_rating");
        facetsCollector = new FacetsCollector();
    }
    // singleton para acceder siempre a la misma instancia
    public static synchronized IndexSearch getInstance(String indexDir) throws IOException {
        if (instance == null) {
            instance = new IndexSearch(indexDir);
        }
        return instance;
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
                QueryParser parserDocType = new QueryParser(SearchOption.DOC_TYPE.getField(), SearchOption.DOC_TYPE.getAnalyzer());
                querieListEpisode.add(new QueryData(parserDocType.parse("episode"), occur));
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

        TopDocs topDocsEpisode = FacetsCollector.search(searcher,queryEpisode,maxResults,facetsCollector);
        TopDocs topDocsDialog = FacetsCollector.search(searcher,queryDialog,maxResults,facetsCollector);
        //TopDocs topDocFacets = FacetsCollector.search(searcher,queryEpisode,maxResults,facetsCollector);

        ArrayList<Document> topDocsEpisodearr = new ArrayList<>();
        ArrayList<Document> topDocsDialogarr = new ArrayList<>();

        for(ScoreDoc episodedoc : topDocsEpisode.scoreDocs)
            topDocsEpisodearr.add(searcher.doc(episodedoc.doc));

        for(ScoreDoc dialogdoc : topDocsDialog.scoreDocs)
            topDocsDialogarr.add(searcher.doc(dialogdoc.doc));



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

    public SearchResult searchAndObtainFacets() throws IOException, ParseException, java.text.ParseException {
        ArrayList<MetricDoc> docs = search();

        Facets facetas = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, facetsCollector);
        List<FacetResult> dims = facetas.getAllDims(100);

        facetsCollector = new FacetsCollector();

        return new SearchResult(docs,dims);

    }

    public ArrayList<MetricDoc> searchDrillDown(String[] facetInputs) throws IOException, ParseException, java.text.ParseException {
        DrillDownQuery drillDownQuery = new DrillDownQuery(facetsConfig,queryDialog);

        for(String input : facetInputs){
            String[] inputSplit = input.split(":");
            drillDownQuery.add(inputSplit[0],inputSplit[1]);
        }


        return generateMetricDocList(FacetsCollector.search(searcher,drillDownQuery,maxResults,facetsCollector));
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

        //TopDocs topDocs = searcher.search(allq, maxResults);
        TopDocs topDocs = FacetsCollector.search(searcher,allq,maxResults,facetsCollector);

        return generateMetricDocList(topDocs);


    }

    public SearchResult globalSearchWithFacets(String globalQuery) throws IOException, ParseException, java.text.ParseException {
        ArrayList<MetricDoc> docs = allFieldsSearch(globalQuery);

        Facets facetas = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, facetsCollector);
        List<FacetResult> dims = facetas.getAllDims(100);

        facetsCollector = new FacetsCollector();

        return new SearchResult(docs,dims);

    }

    private ArrayList<MetricDoc> generateMetricDocList( TopDocs topDocs) throws IOException, ParseException, java.text.ParseException {
        ArrayList<MetricDoc> metricDocList = new ArrayList<>();

        for (ScoreDoc doc : topDocs.scoreDocs)
            metricDocList.add(new MetricDoc(searcher.doc(doc.doc)));

        return metricDocList;
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
