package indexsearcher;

import org.apache.lucene.facet.FacetResult;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {
    List<FacetResult> dims;
    ArrayList<MetricDoc> docs;

    public SearchResult(ArrayList<MetricDoc> docs, List<FacetResult> dims) {
        this.docs = docs;
        this.dims = dims;
    }

    public List<FacetResult> getDims() {
        return dims;
    }

    public void setDims(List<FacetResult> dims) {
        this.dims = dims;
    }

    public ArrayList<MetricDoc> getDocs() {
        return docs;
    }

    public void setDocs(ArrayList<MetricDoc> docs) {
        this.docs = docs;
    }
}
