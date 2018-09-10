package edu.vt.cs.ir.hw1;

//import com.sun.java.util.jar.pack.ConstantPool;
import edu.vt.cs.ir.utils.LuceneUtils;
import edu.vt.cs.ir.utils.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermFrequencyAttribute;
import org.apache.lucene.document.Document;
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

        for (int i=0; i<queryTerms.size(); i++) {
            String queryTerm = queryTerms.get(i);

            int nw = 0;
            int N = index.maxDoc();

            // todo: delete
            ArrayList<String> printLines = new ArrayList<>();

            /* compute nw */
            PostingsEnum posting = MultiFields.getTermDocsEnum(index, "text", new BytesRef(queryTerm), PostingsEnum.FREQS);
            if (posting != null) {
                int docid;
                while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    String docno = LuceneUtils.getDocno(index, "docno", docid);
                    int freq = posting.freq();

                    // todo: delete
                    printLines.add(docno + '\t' + Integer.toString(freq));

                    if (freq > 0) {
                        nw += 1;
                    }

                }
            }

            if (queryTerm.equals("query"))
                TestHw1.printToFile(printLines,"part2Query"); // todo: delete

            /* get search results */
            posting = MultiFields.getTermDocsEnum(index, "text", new BytesRef(queryTerm), PostingsEnum.FREQS);
            if (posting != null) {
                int docid;
                while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    String docno = LuceneUtils.getDocno(index, "docno", docid);
                    int freq = posting.freq();

                    double sumTerm = (double)freq * Math.log((double) N / (double) nw);

                    if (docno.equals("ACM-2124339")) {
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

    static class DocTermFreq {
        private Map<String, Integer> terms;

        DocTermFreq() {
            terms = new HashMap<>();
        }

        public boolean contains(String term) {
            return terms.containsKey(term);
        }
        public int get(String term) {
            return terms.get(term);
        }
        public void add(String term, int quantity) {
            terms.put(term, quantity);
        }

    }
    private static Map<Integer, DocTermFreq> x = null;

    private static int getTermFreqInDoc(String term, int docid, IndexReader index) {
        int docidWeWant = docid;


        /* see if we don't even need to run through all the BS below */
        if (x == null)
            x = new HashMap<>();

        x.putIfAbsent(docid, new DocTermFreq());

        if (x.get(docid).contains(term)) {
            return x.get(docid).get(term);
        }

        try {
            String field = "text";

            // The following line reads the posting list of the term in a specific index field.
            // You need to encode the term into a BytesRef object,
            // which is the internal representation of a term used by Lucene.
            PostingsEnum posting = MultiFields.getTermDocsEnum(index, field, new BytesRef(term), PostingsEnum.FREQS);
            if (posting != null) { // if the term does not appear in any document, the posting object may be null
                // Each time you call posting.nextDoc(), it moves the cursor of the posting list to the next position
                // and returns the docid of the current entry (document). Note that this is an internal Lucene docid.
                // It returns PostingsEnum.NO_MORE_DOCS if you have reached the end of the posting list.
                while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
//                    System.out.println("\t" + Integer.toString(docid)); // todo: delete
                    // make sure it actually exists
                    x.putIfAbsent(docid, new DocTermFreq());

                    String docno = LuceneUtils.getDocno(index, "docno", docid);
                    int freq = posting.freq(); // get the frequency of the term in the current document

                    x.get(docid).add(term, freq);

                }
            }
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }


        return x.get(docidWeWant).get(term);
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

        List<SearchResult> searchResults = new ArrayList<>();

        for (int i=0; i<index.maxDoc(); i++) {
//            System.out.println(i);  // todo: delete
            Document doc = index.document(i);
            Terms termVector = index.getTermVector(i, "text");
            TermsEnum termsEnum = termVector.iterator();

//            String nextTerm = termsEnum.next().utf8ToString();
//            double totalFreq = 0;
//            int testi = 0; // todo: delete
//            while (nextTerm != null) {
//                int freq = getTermFreqInDoc(nextTerm, i, index);
//                totalFreq += Math.pow((double) freq, 2);
//                nextTerm = termsEnum.next().utf8ToString();
//                System.out.println(testi ++);   // todo: delete
//            }

            String nextTerm;
            BytesRef nextBytesRefTerm = termsEnum.next();
            double totalFreq = 0;
            int testi = 0; // todo: delete
            while (nextBytesRefTerm != null) {
                System.out.println("Next term.");
                nextTerm = nextBytesRefTerm.utf8ToString();
                int freq = getTermFreqInDoc(nextTerm, i, index);
                totalFreq += Math.pow((double) freq, 2);
                nextBytesRefTerm = termsEnum.next();
//                System.out.println(testi ++);   // todo: delete
            }

            totalFreq = Math.sqrt(totalFreq);
            String docno = LuceneUtils.getDocno( index, "docno", i );

            double score = 2.0 / totalFreq;

            searchResults.add(new SearchResult(i, docno, score));
        }







        return null;
    }



}
