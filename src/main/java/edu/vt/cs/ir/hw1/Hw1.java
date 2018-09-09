package edu.vt.cs.ir.hw1;

/*
* This will act as the main method for the Java class.
*
*
 */


//import java.io.FileReader;
//import java.io.BufferedReader;
import edu.vt.cs.ir.utils.LuceneUtils;
import edu.vt.cs.ir.utils.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class Hw1 {
//    public static String FILEPATH = "/Users/stephenlasky/Documents/cs5604/CS5604_HW1_b/CS5604_HW1/acm_corpus_med";
    public static String FILEPATH = "/Users/stephenlasky/Documents/cs5604/CS5604_HW1_b/CS5604_HW1/acm_corpus";

    public static void main(String[] args) {
        System.out.println("Hello Stephen Lasky. Main program started.");

//        part1();
//        part1_2();
//        part1_3();

//        part2();

//        test();
//        test2();

        TestHw1.test3();
    }

    private static char[] input;
    private static long inputSize;
    public static ArrayList<Document> documents;

    private static String DOC_START = "<DOC>";
    private static String DOC_END = "</DOC>";

    private static String DOCNO_START = "<DOCNO>";
    private static String DOCNO_END = "</DOCNO>";

    private static String TEXT_START = "<TEXT>";
    private static String TEXT_END = "</TEXT>";
    /*
     * Please Note: File opening code is courtesy of https://www.caveofprogramming.com/java/java-file-reading-and-writing-files-in-java.html
     * Also, character count courtesy of https://stackoverflow.com/questions/15240504/counting-a-specific-character-in-a-file-with-scanner
     */
    private static void openFile() {
        /* get file length and initialize char array */
        File file = new File(FILEPATH);
        inputSize = file.length();
        input = new char[(int)inputSize];
        int i = 0;

        // The name of the file to open.
        String fileName = FILEPATH;

        // This will reference one line at a time
        String line = null;

        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader =
                    new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader =
                    new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                // place string into character array
                for (int j=0; j < line.length(); j++) {
//                    System.out.println("i:" + Integer.toString(i) + " j: " + Integer.toString(j));
                    input[i] = line.charAt(j);
                    i++;
                }
            }

            // Always close files.
            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            fileName + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + fileName + "'");
            // Or we could just do this:
            // ex.printStackTrace();
        }
        inputSize = i;
    }
    private static void printInputChars(int i, int j) {
        int stop = i + j;
        String out = "";
        for (i=i; i<stop; i++) {
            out += input[i];
        }

        System.out.println(out);
    }
    private static int findNextMatch(String find, int start) {
//        System.out.println("Trying out (" + find + ") " + Integer.toString(start));
        int findLength = find.length();
        char[] findArr = find.toCharArray();

        int i;
        while (true) {
            for (i=0; i<findLength; i++) {
                if (input[start+i] != findArr[i]){
                    break;
                }
            }

            if (i == findLength) {
                return start;
            }

//            if (Arrays.equals(findArr, Arrays.copyOfRange(input, start, start + findLength))) {
//                return start;
//            }

            start += 1;
        }
    }

    public static void part1() {
        final long startTime = System.currentTimeMillis();
        System.out.println("Part 1 begin.");

        /* get file */
        System.out.println("Reading file.");
        openFile();
        System.out.println("End reading file.");

        String docno = "";
        String text = "";
        int i = 0;
        int test = 0;

        int docnoEnd;
        int textEnd;

        documents = new ArrayList<Document>();

        /* place all of the documents into the Document class */
        float alertInc = (float)0.05;
        float nextAlertInc = alertInc;
        while (i<inputSize) {

            /* start of doc section */
            i = findNextMatch(DOC_START, i) + DOC_START.length();

            /* start of docno section */
            i = findNextMatch(DOCNO_START, i) + DOCNO_START.length();

            /* get docno */
            docnoEnd = findNextMatch(DOCNO_END, i);
            while (i != docnoEnd) {
                docno += input[i];
                i ++;
            }
            i += DOCNO_END.length();

            /* get text */
            i = findNextMatch(TEXT_START, i) + TEXT_START.length();
            textEnd = findNextMatch(TEXT_END, i);
            while (i != textEnd) {
                text += input[i];
                i ++;
            }
            i += TEXT_END.length();

            /* finally, zoom forward to the end of the doc */
            i = findNextMatch(DOC_END, i) + DOC_END.length();

            /* put them into a document class, and remember them */
            documents.add(new Document(docno, text));

            /* cleanup and reset */
            docno = "";
            text = "";

            if ((inputSize - i) < DOC_START.length())   // ensures we stop if it's not possible to find anything more
                break;

            if ((float)i / (float)inputSize > nextAlertInc) {
                System.out.println(nextAlertInc);
                nextAlertInc += alertInc;
            }
        }



        /* compute some statistics */
        float averageTextLength = 0;
        int longestDocWords = 0;
        String longestDocNo = "";
        Map<String, Integer> words = new HashMap<String, Integer>();
        int informationFreq = 0;
        int retrievalFreq = 0;
        for (i=0; i<documents.size(); i++) {
            /* do average length stuff */
            Document currDoc = documents.get(i);
            averageTextLength += currDoc.getTextFieldLen();

            /* put words into word dictionary */
            for (int j=0; j<currDoc.getNumTokens(); j++) {
                if (words.putIfAbsent(currDoc.getTokenAtIndex(j), 1) != null)
                    words.put(currDoc.getTokenAtIndex(j), words.get(currDoc.getTokenAtIndex(j)) + 1);
            }

            /* compute longest doc */
            if (currDoc.getTextFieldLen() > longestDocWords) {
                longestDocWords = currDoc.getTextFieldLen();
                longestDocNo = currDoc.getDocno();
            }

            /* information & retrieval info */
            if (currDoc.contains("retrieval"))
                retrievalFreq ++;
            if (currDoc.contains("information"))
                informationFreq ++;
        }
        averageTextLength /= documents.size();
        int numUniqueWords = words.size();
        double retrievalIdf = Math.log((float) documents.size() / (float)retrievalFreq);
        double informationIdf = Math.log((float) documents.size() / (float)informationFreq);
        final long endTime = System.currentTimeMillis();


        /* print some statistics */
        System.out.println("*** Part 1.1 STATS ***");
        System.out.println("\tNumber of documents:       " + Integer.toString(documents.size()));
        System.out.println("\tAverage text field length: " + Float.toString(averageTextLength));
        System.out.println("\tNumber of unique words:    " + Integer.toString(numUniqueWords));
        System.out.println("\tLongest docno / words:     " + longestDocNo + " / " + Integer.toString(longestDocWords));
        System.out.println("\t'retrieval freq/IDF:       " + Integer.toString(retrievalFreq) + " / " + Double.toString(retrievalIdf));
        System.out.println("\t'information freq/IDF:     " + Integer.toString(informationFreq) + " / " + Double.toString(informationIdf));
        System.out.println("\tTotal execution time:      " + (endTime - startTime) );

        /* finally finished */
        System.out.println(">>> Part 1.1 complete.");

        /* part 1.2 */

    }
    public static void part1_2() {
        System.out.println("Part 1.2 started.");
        try {

            String pathCorpus = "./acm_corpus"; // path of the corpus file
            String pathIndex = "./"; // path of the index directory

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

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

            IndexWriterConfig config = new IndexWriterConfig( analyzer );
            // Note that IndexWriterConfig.OpenMode.CREATE will override the original index in the folder
            config.setOpenMode( IndexWriterConfig.OpenMode.CREATE );

            IndexWriter ixwriter = new IndexWriter( dir, config );

            // This is the field setting for metadata field.
            FieldType fieldTypeMetadata = new FieldType();
            fieldTypeMetadata.setOmitNorms( true );
            fieldTypeMetadata.setIndexOptions( IndexOptions.DOCS );
            fieldTypeMetadata.setStored( true );
            fieldTypeMetadata.setTokenized( false );
            fieldTypeMetadata.freeze();

            // This is the field setting for normal text field.
            FieldType fieldTypeText = new FieldType();
            fieldTypeText.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
            fieldTypeText.setStoreTermVectors( true );
            fieldTypeText.setStoreTermVectorPositions( true );
            fieldTypeText.setTokenized( true );
            fieldTypeText.setStored( true );
            fieldTypeText.freeze();

            // You need to iteratively read each document from the corpus file,
            // create a Document object for the parsed document, and add that
            // Document object by calling addDocument().

            // write your impelemntation here
            String docno = "";
            String text = "";
            int i = 0;
            int test = 0;

            int docnoEnd;
            int textEnd;

            while (i<inputSize) {

                /* start of doc section */
                i = findNextMatch(DOC_START, i) + DOC_START.length();

                /* start of docno section */
                i = findNextMatch(DOCNO_START, i) + DOCNO_START.length();

                /* get docno */
                docnoEnd = findNextMatch(DOCNO_END, i);
                while (i != docnoEnd) {
                    docno += input[i];
                    i ++;
                }
                i += DOCNO_END.length();

                /* get text */
                i = findNextMatch(TEXT_START, i) + TEXT_START.length();
                textEnd = findNextMatch(TEXT_END, i);
                while (i != textEnd) {
                    text += input[i];
                    i ++;
                }
                i += TEXT_END.length();

                /* finally, zoom forward to the end of the doc */
                i = findNextMatch(DOC_END, i) + DOC_END.length();

                /* add document to the index */
                org.apache.lucene.document.Document d = new org.apache.lucene.document.Document();
                d.add( new Field( "docno", docno, fieldTypeMetadata ) );
                d.add( new Field( "text", text, fieldTypeText ) );
                ixwriter.addDocument( d );


                /* cleanup and reset */
                docno = "";
                text = "";

                if ((inputSize - i) < DOC_START.length())   // ensures we stop if it's not possible to find anything more
                    break;
            }

            // remember to close both the index writer and the directory
            ixwriter.close();
            dir.close();



        } catch ( Exception e ) {
            e.printStackTrace();
        }
        System.out.println("Part 1.2 finished.");
    }
    public static void part1_3() {
        final long startTime = System.currentTimeMillis();
        System.out.println("Part 3 START.");

        String pathIndex = "./"; // path of the index directory

        try {
            Directory dir = FSDirectory.open(new File(pathIndex).toPath());
            IndexReader index = DirectoryReader.open(dir);

            /* compute some statistics */
            int numDocs = index.numDocs();
            float averageDocLength = 0;
            int thisDocLength = 0;
            int longestDocLength = 0;
            String longestDocno = "";
            Set<String> fieldset = new HashSet<>();
            fieldset.add("docno");
            int informationDocFreq = 0;
            int retrievalDocFreq = 0;
            boolean containsInformation = false;
            boolean containsRetrieval = false;
            for (int i = 0; i < index.maxDoc(); i++) {
                String docno = index.document(i, fieldset).get("docno");
                TermsEnum termsEnum = index.getTermVector(i, "text").iterator();
                while (termsEnum.next() != null) {
                    thisDocLength += termsEnum.totalTermFreq();
                    if (termsEnum.term().utf8ToString().equals("information"))
                        containsInformation = true;
                    if (termsEnum.term().utf8ToString().equals("retrieval"))
                        containsRetrieval = true;
                }
                averageDocLength += thisDocLength;

                if (thisDocLength > longestDocLength) {
                    longestDocLength = thisDocLength;
                    longestDocno = docno;
                }
                thisDocLength = 0;

                if (containsInformation) {
                    informationDocFreq ++;
                    containsInformation = false;
                }
                if (containsRetrieval) {
                    retrievalDocFreq ++;
                    containsRetrieval = false;
                }

                /* get postings */

            }
            averageDocLength /= numDocs;

            int numUniqueTerms = 0;
            Fields fields = MultiFields.getFields(index);
            Terms terms = fields.terms("text");
            TermsEnum iterator = terms.iterator();
            BytesRef byteRef = null;
            while((byteRef = iterator.next()) != null) {
                String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
                numUniqueTerms ++;
            }

            double retrievalIdf = Math.log((float) numDocs / (float)retrievalDocFreq);
            double informationIdf = Math.log((float) numDocs / (float)informationDocFreq);

            PostingsEnum queryPosting = MultiFields.getTermDocsEnum(index, "text", new BytesRef("query"), PostingsEnum.FREQS);
            PostingsEnum reformulationPosting = MultiFields.getTermDocsEnum(index, "text", new BytesRef("reformulation"), PostingsEnum.FREQS);
            ArrayList<String> queryPostingArr = new ArrayList<String>();
            ArrayList<String> reformulationPostingArr = new ArrayList<String>();
            ArrayList<String> queryAndReforumlation = new ArrayList<String>();
            int docid;
            if (queryPosting != null) {
                while ((docid = queryPosting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    queryPostingArr.add(LuceneUtils.getDocno(index, "docno", docid));
                }
            }
            if (reformulationPosting != null) {
                while ((docid = reformulationPosting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    reformulationPostingArr.add(LuceneUtils.getDocno(index, "docno", docid));
                }
            }
            for (int i=0; i<queryPostingArr.size(); i++) {
                for (int j=0; j<reformulationPostingArr.size(); j++) {
                    if (queryPostingArr.get(i).equals(reformulationPostingArr.get(j))) {
                        queryAndReforumlation.add(queryPostingArr.get(i));
                    }
                }
            }

            final long endTime = System.currentTimeMillis();    // get end time




            /* print some statistics */
            System.out.println("\tNumber of documents:       " + Integer.toString(numDocs));
            System.out.println("\tAverage doc length:        " + Float.toString(averageDocLength));
            System.out.println("\tNum unique terms:          " + Integer.toString(numUniqueTerms));
            System.out.println("\tLongest docno/length:      " + longestDocno + "/" + longestDocLength);
            System.out.println("\t'retrieval' freq/IDF:      " + retrievalDocFreq + "/" + Double.toString(retrievalIdf));
            System.out.println("\t'information' freq/IDF:    " + informationDocFreq + "/" + Double.toString(informationIdf));

            System.out.println();
            System.out.println("\t'query' doc freq:          " + Integer.toString(queryPostingArr.size()));
            System.out.println("\t'reformulation' doc freq:  " + Integer.toString(reformulationPostingArr.size()));
            System.out.println("\tdocs containing BOTH:      " + Integer.toString(queryAndReforumlation.size()));
            for (int i=0; i<queryAndReforumlation.size(); i++)
                System.out.println("\t\t" + queryAndReforumlation.get(i));
            System.out.println("\tfinal execution time       " + (endTime - startTime));


            /* all done */
            index.close();
            dir.close();

        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public static void part2() {
        System.out.println("Part 2 started.");
        LuceneSearchIndex.main(null);
    }

    public static void test() {
        /* get file */
        System.out.println("Reading file.");
        openFile();
        System.out.println("End reading file.");

        String docno = "";
        String text = "";
        int i = 0;
        int test = 0;

        int docnoEnd;
        int textEnd;

        documents = new ArrayList<Document>();

        /* place all of the documents into the Document class */
        float alertInc = (float)0.05;
        float nextAlertInc = alertInc;
        while (i<inputSize) {
            /* start of doc section */
            i = findNextMatch(DOC_START, i) + DOC_START.length();

            /* start of docno section */
            i = findNextMatch(DOCNO_START, i) + DOCNO_START.length();

            /* get docno */
            docnoEnd = findNextMatch(DOCNO_END, i);
            while (i != docnoEnd) {
                docno += input[i];
                i ++;
            }
            i += DOCNO_END.length();

            /* get text */
            i = findNextMatch(TEXT_START, i) + TEXT_START.length();
            textEnd = findNextMatch(TEXT_END, i);
            while (i != textEnd) {
                text += input[i];
                i ++;
            }
            i += TEXT_END.length();

            /* finally, zoom forward to the end of the doc */
            i = findNextMatch(DOC_END, i) + DOC_END.length();

            /* put them into a document class, and remember them */
            documents.add(new Document(docno, text));

            /* cleanup and reset */
            docno = "";
            text = "";

            if ((inputSize - i) < DOC_START.length())   // ensures we stop if it's not possible to find anything more
                break;

            if ((float)i / (float)inputSize > nextAlertInc) {
                System.out.println(nextAlertInc);
                nextAlertInc += alertInc;
            }
        }
        /* finished processing doc */



        /* test stuff here */
        Map<String, Double> docnoToScore = new HashMap<String, Double>();
        ArrayList<String> tokens = new ArrayList<String>();
        tokens.add("query");
        tokens.add("reformulation");

        Map<String, Integer> tokensDocFreq = new HashMap<>();
        for (String token: tokens)
            tokensDocFreq.put(token, 0);

        int nw;
        int numDocs = documents.size();
        double score;

        for (String token: tokens) {
            /* first compute nw */
            nw = 0;
            for (i=0; i<numDocs; i++) {
                if (documents.get(i).contains(token))
                    nw ++;
            }



            /* now compute the score for each document */
            for (i=0; i<numDocs; i++) {
                docno = documents.get(i).getDocno();

                score = 0;
                score = (double) documents.get(i).containsCount(token);
                score *= Math.log((double)numDocs / (double) nw);

                // todo: remove
//                if (docno.equals("ACM-2609463")) {
//                    String output = "";
//                    output += "token" + token + " ";
//                    output += "f:" + Integer.toString(documents.get(i).containsCount(token)) + " ";
//                    output += "N:" + Integer.toString(N)
//
//                    System.out.println(output);
//                }

                if (docnoToScore.putIfAbsent(docno, score) != null) {
                    score += docnoToScore.get(docno);
                    docnoToScore.put(docno, score);
                }
            }
        }

        /* FINALLY finished scoring. Let's now rank them */
        List<SearchResult> finalResults = new ArrayList<>();
        for (String key : docnoToScore.keySet()) {
            docno = key;

            finalResults.add(new SearchResult(-1, docno, docnoToScore.get(docno)));
        }

        /* sort the final list */
        Collections.sort(finalResults, new Comparator<SearchResult>() {
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

        /* print the top 20 */
        for (i=0; i<20; i++) {
            System.out.println(finalResults.get(i).getDocno() + "\t" + Double.toString(finalResults.get(i).getScore()));
        }
    }

    public static void loadDocuments() {
        /* get file */
        System.out.println("Reading file.");
        openFile();
        System.out.println("End reading file.");

        String docno = "";
        String text = "";
        int i = 0;
        int test = 0;

        int docnoEnd;
        int textEnd;

        documents = new ArrayList<Document>();

        /* place all of the documents into the Document class */
        float alertInc = (float)0.05;
        float nextAlertInc = alertInc;
        while (i<inputSize) {
            /* start of doc section */
            i = findNextMatch(DOC_START, i) + DOC_START.length();

            /* start of docno section */
            i = findNextMatch(DOCNO_START, i) + DOCNO_START.length();

            /* get docno */
            docnoEnd = findNextMatch(DOCNO_END, i);
            while (i != docnoEnd) {
                docno += input[i];
                i ++;
            }
            i += DOCNO_END.length();

            /* get text */
            i = findNextMatch(TEXT_START, i) + TEXT_START.length();
            textEnd = findNextMatch(TEXT_END, i);
            while (i != textEnd) {
                text += input[i];
                i ++;
            }
            i += TEXT_END.length();

            /* finally, zoom forward to the end of the doc */
            i = findNextMatch(DOC_END, i) + DOC_END.length();

            /* put them into a document class, and remember them */
            documents.add(new Document(docno, text));

            /* cleanup and reset */
            docno = "";
            text = "";

            if ((inputSize - i) < DOC_START.length())   // ensures we stop if it's not possible to find anything more
                break;

            if ((float)i / (float)inputSize > nextAlertInc) {
                System.out.println(nextAlertInc);
                nextAlertInc += alertInc;
            }
        }
        /* finished processing doc */
    }

    public static void test2() {
        loadDocuments();

        String token = "query";
        String docno;
        String quantityStr;
        String printLine;

        ArrayList<String> printStuff = new ArrayList<>();

        for (int i=0; i<documents.size(); i++) {
            docno = documents.get(i).getDocno();
            quantityStr = Integer.toString(documents.get(i).containsCount(token));

            printLine = docno + "\t" + quantityStr;
            printStuff.add(printLine);
        }

        TestHw1.printToFile(printStuff, "part1Query");
    }




}
