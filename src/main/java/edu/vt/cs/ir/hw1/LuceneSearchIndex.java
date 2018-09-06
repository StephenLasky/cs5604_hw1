package edu.vt.cs.ir.hw1;

import edu.vt.cs.ir.utils.LuceneUtils;
import edu.vt.cs.ir.utils.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.*;

public class LuceneSearchIndex {

    public static void main( String[] args ) {
        try {

            // your own index path
            String pathIndex = "./";
            // the folder to output your search results
            String pathOutput = "./";

            // the query and the field to search the query
            String field = "text";
            String query = "query reformulation";

            // Analyzer includes options for text processing
            Analyzer analyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents( String fieldName ) {
                    // Step 1: tokenization (Lucene's StandardTokenizer is suitable for most text retrieval occasions)
                    TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
                    // Step 2: transforming all tokens into lowercased ones
                    ts = new TokenStreamComponents( ts.getTokenizer(), new LowerCaseFilter( ts.getTokenStream() ) );
                    // Step 3: whether to remove stop words
                    // Uncomment the following line to remove stop words
                    // ts = new TokenStreamComponents( ts.getTokenizer(), new StopwordsFilter( ts.getTokenStream(), StandardAnalyzer.ENGLISH_STOP_WORDS_SET ) );
                    // Step 4: whether to apply stemming
                    // Uncomment the following line to apply Krovetz or Porter stemmer
                    // ts = new TokenStreamComponents( ts.getTokenizer(), new KStemFilter( ts.getTokenStream() ) );
                    // ts = new TokenStreamComponents( ts.getTokenizer(), new PorterStemFilter( ts.getTokenStream() ) );
                    return ts;
                }
            };

            // tokenize the query into words (make sure you are using the same analyzer you used for indexing)
            List<String> queryTerms = LuceneUtils.tokenize( query, analyzer );

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader index = DirectoryReader.open( dir );

            List<SearchResult> resultsBooleanAND = searchBooleanAND( index, field, queryTerms );
            List<SearchResult> resultsTFIDF = searchTFIDF( index, field, queryTerms );
            List<SearchResult> resultsVSMCosine = searchVSMCosine( index, field, queryTerms );

            // do not change the following outputs
            File dirOutput = new File( pathOutput );
            dirOutput.mkdirs();

            {
                PrintStream writer = new PrintStream( new FileOutputStream( new File( dirOutput, "results_BooleanAND" ) ) );
                SearchResult.writeTRECFormat( writer, "0", "BooleanAND", resultsBooleanAND, resultsBooleanAND.size() );
                SearchResult.writeTRECFormat( System.out, "0", "BooleanAND", resultsBooleanAND, 10 );
                writer.close();
            }

            {
                PrintStream writer = new PrintStream( new FileOutputStream( new File( dirOutput, "results_TFIDF" ) ) );
                SearchResult.writeTRECFormat( writer, "0", "TFIDF", resultsTFIDF, resultsTFIDF.size() );
                SearchResult.writeTRECFormat( System.out, "0", "TFIDF", resultsTFIDF, 10 );
                writer.close();
            }

            {
                PrintStream writer = new PrintStream( new FileOutputStream( new File( dirOutput, "results_VSMCosine" ) ) );
                SearchResult.writeTRECFormat( writer, "0", "VSMCosine", resultsVSMCosine, resultsVSMCosine.size() );
                SearchResult.writeTRECFormat( System.out, "0", "VSMCosine", resultsVSMCosine, 10 );
                writer.close();
            }

            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Perform a Boolean AND search and return the search results.
     *
     * @param index      A Lucene index reader.
     * @param field      The index field to search the query.
     * @param queryTerms A list of tokenized query terms.
     * @return A list of search results (sorted by relevance scores).
     */
    public static List<SearchResult> searchBooleanAND( IndexReader index, String field, List<String> queryTerms ) throws Exception {
        // Write you implementation Problem 2 "Boolean AND" here
        ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
        List<String> queryTermsNew = new ArrayList<String>();
        for (int i=0; i<queryTerms.size(); i++)
            queryTermsNew.add(new String(queryTerms.get(i)));

        /* instantiate all the lists */
        Map<String, Integer> docids = new HashMap<String, Integer>();
        boolean doDocIds = true;
        while (queryTermsNew.size() > 0) {
            String queryTerm = queryTermsNew.remove(0);
            ArrayList<String> newList = new ArrayList<String>();

            PostingsEnum posting = MultiFields.getTermDocsEnum(index, "text", new BytesRef(queryTerm), PostingsEnum.FREQS);
            if (posting != null) {
                int docid;
                while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    String docno = LuceneUtils.getDocno(index, "docno", docid);
                    newList.add(docno);

                    if (doDocIds)
                        docids.put(docno, docid);
                }
            }
            lists.add(newList);
            doDocIds = false;
        }

        /* now go through and eliminate the lists */
        if (lists == null)
            return null;

        List<String> xList = lists.remove(0);
        ArrayList<String> yList = null;
        while (lists.size() > 0) {
            ArrayList<String> newXList = new ArrayList<String>();
            yList = lists.remove(0);
            for (int i=0; i<xList.size(); i++) {
                for (int j=0; j<yList.size(); j++) {
                    if (xList.get(i).equals(yList.get(j)))
                        newXList.add(xList.get(i));
                }
            }

            xList = newXList;
        }

        Collections.sort(xList);
        for (int i=0; i<xList.size(); i++)
            System.out.println(xList.get(i));

        List<SearchResult> returnList = new ArrayList<SearchResult>();
        for (int i=0; i<xList.size(); i++) {
            int docid = docids.get(xList.get(i));
            returnList.add(new SearchResult(docid, xList.get(i), (double) i ));
        }






        return returnList;
    }

    /**
     * Perform a TFxIDF search and return the search results.
     *
     * @param index      A Lucene index reader.
     * @param field      The index field to search the query.
     * @param queryTerms A list of tokenized query terms.
     * @return A list of search results (sorted by relevance scores).
     */
    public static List<SearchResult> searchTFIDF( IndexReader index, String field, List<String> queryTerms ) throws IOException {
        // Write you implementation Problem 2 "TFxIDF" here
        ArrayList<SearchResult> searchResults = new ArrayList<SearchResult>();
        Map<Integer, SearchResult> docidToSearchResult = new HashMap<Integer, SearchResult>();

        while (queryTerms.size() > 0) {
            String queryTerm = queryTerms.remove(0);

            int nw = 0;
            int N = index.maxDoc();

            /* compute nw */
            PostingsEnum posting = MultiFields.getTermDocsEnum(index, "text", new BytesRef(queryTerm), PostingsEnum.FREQS);
            if (posting != null) {
                int docid;
                while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    String docno = LuceneUtils.getDocno(index, "docno", docid);
                    int freq = posting.freq();

                    if (freq > 0)
                        nw += 1;
                }
            }

            /* get search results */
            posting = MultiFields.getTermDocsEnum(index, "text", new BytesRef(queryTerm), PostingsEnum.FREQS);
            if (posting != null) {
                int docid;
                while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    String docno = LuceneUtils.getDocno(index, "docno", docid);
                    int freq = posting.freq();

                    double sumTerm = (double)freq * Math.log((double) N / (double) nw);

                    if (docno.equals("ACM-1718493")) {
                        // break here
                        int x = 5;
                    }

                    if (docidToSearchResult.putIfAbsent(docid, new SearchResult(docid, docno, sumTerm)) != null) {
                        sumTerm += docidToSearchResult.get(docid).getScore();
                        docidToSearchResult.put(docid, new SearchResult(docid, docno, sumTerm));
                    }
                }
            }


        }

        /* note: iterator code motivated by example here: https://stackoverflow.com/questions/1066589/iterate-through-a-hashmap */
        Iterator it = docidToSearchResult.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            searchResults.add((SearchResult)pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }

        Collections.sort(searchResults, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult o1, SearchResult o2) {
                if (o1.getScore() > o2.getScore())
                    return -1;
                else if (o1.getScore() < o2.getScore())
                    return 1;
                else
                    return 0;
            };
        });

        /* assert correctly sorted */
//        for (int i=0; i<searchResults.size() - 1; i++) {
//            if (searchResults.get(i).getScore() > searchResults.get(i+1).getScore())
//                System.out.println("Good: " + Double.toString(searchResults.get(i).getScore()) + " > " + Double.toString(searchResults.get(i+1).getScore()));
//            else
//                System.out.println("Bad: " + Double.toString(searchResults.get(i).getScore()) + " < " + Double.toString(searchResults.get(i+1).getScore()));
//
//        }

        // todo: delete this debug information
        for (int i=0; i<searchResults.size(); i++) {
            if (searchResults.get(i).getDocno().equals("ACM-1718493")) {
                System.out.println("ACM-1718493 is " + Double.toString(searchResults.get(i).getScore()));
            }

            if (searchResults.get(i).getDocno().equals("ACM-2187890")) {
                System.out.println("ACM-2187890 is " + Double.toString(searchResults.get(i).getScore()));
            }
            if (searchResults.get(i).getDocno().equals("ACM-2124339")) {
                System.out.println("ACM-2124339 is " + Double.toString(searchResults.get(i).getScore()));
            }
        }

        return searchResults;
    }

    /**
     * Perform a VSM (cosine similarity) search and return the search results.
     *
     * @param index      A Lucene index reader.
     * @param field      The index field to search the query.
     * @param queryTerms A list of tokenized query terms.
     * @return A list of search results (sorted by relevance scores).
     */
    public static List<SearchResult> searchVSMCosine( IndexReader index, String field, List<String> queryTerms ) throws IOException {
        // Write you implementation Problem 2 "VSM (cosine similarity)" here
        return null;
    }

}
