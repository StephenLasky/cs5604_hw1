package edu.vt.cs.ir.hw1;

/*
* This will act as the main method for the Java class.
*
*
 */


//import java.io.FileReader;
//import java.io.BufferedReader;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

public class Hw1 {
    public static String FILEPATH = "/Users/stephenlasky/Documents/cs5604/CS5604_HW1_b/CS5604_HW1/acm_corpus";
//    public static String FILEPATH = "/Users/stephenlasky/Documents/cs5604/CS5604_HW1_b/CS5604_HW1/acm_corpus_small";

    public static void main(String[] args) {
        System.out.println("Hello Stephen Lasky.");

        part1();

    }

    private static char[] input;
    private static long inputSize;
    private static ArrayList<Document> documents;

    private static String DOC_START = "<DOC>";
    private static String DOC_END = "</DOC>";

    private static String DOCNO_START = "<DOCNO>";
    private static String DOCNO_END = "</DOCNO>";

    private static String TEXT_START = "<TEXT>";
    private static String TEXT_END = "</TEXT>";
    /*
     * Please Note: File opening code is courtesy of https://www.caveofprogramming.com/java/java-file-reading-and-writing-files-in-java.html
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

        int i;
        while (true) {
            for (i=0; i<find.length(); i++) {
                if (input[start+i] != find.charAt(i)){
                    break;
                }
            }

            if (i == find.length()) {
//                System.out.println(find + " @ " + Integer.toString(start));   // todo: only here for debugging
                return start;
            }

            start += 1;
        }
    }

    public static void part1() {
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
        float alertInc = (float)0.01;
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

            /* keep track of progress alert */
            if ((float) i / (float) inputSize > nextAlertInc) {
                System.out.println("processed " + Float.toString((float) i / (float)inputSize) + "/1.00");
                nextAlertInc += alertInc;
            }
        }

        System.exit(0);
        System.out.println("Exiting...");

        /* print some test stuff */
//        Document testDoc = documents.get(0);
//        System.out.println(testDoc.getText());
//        testDoc.printTokens();

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
            if (currDoc.contains("retreival"))
                retrievalFreq ++;
            if (currDoc.contains("information"))
                retrievalFreq ++;
        }
        averageTextLength /= documents.size();
        int numUniqueWords = words.size();
        double retrievalIdf = Math.log((float) documents.size() / (float)retrievalFreq);
        double informationIdf = Math.log((float) documents.size() / (float)informationFreq);

        /* print some statistics */
        System.out.println("Number of documents:       " + Integer.toString(documents.size()));
        System.out.println("Average text field length: " + Float.toString(averageTextLength));
        System.out.println("Number of unique words:    " + Integer.toString(numUniqueWords));
        System.out.println("Longest docno / words:     " + longestDocNo + " / " + Integer.toString(longestDocWords));
        System.out.println("'retrieval freq/IDF:       " + Integer.toString(retrievalFreq) + " / " + Double.toString(retrievalIdf));
        System.out.println("'information freq/IDF:     " + Integer.toString(informationFreq) + " / " + Double.toString(informationIdf));

        /* finally finished */
        System.out.println("Part 1 complete.");
    }



}
