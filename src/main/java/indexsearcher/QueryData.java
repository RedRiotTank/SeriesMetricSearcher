package indexsearcher;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

public class QueryData {
    private Query query;
    private BooleanClause.Occur occur;


    QueryData(Query query, BooleanClause.Occur occur){
        this.query = query;
        this.occur = occur;
    }

    public Query getQuery() {
        return query;
    }

    public BooleanClause.Occur getOccur() {
        return occur;
    }

}
