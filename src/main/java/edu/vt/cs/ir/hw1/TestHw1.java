package edu.vt.cs.ir.hw1;

import edu.vt.cs.ir.utils.SearchResult;

// todo: FIX the bad docno parsing.

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.*;

public class TestHw1 {
    public static ArrayList<Document> documents;

    public TestHw1() {
        /* load the documents */
    }

    public static boolean printToFile(ArrayList<String> lines, String filename) {
        try {
            PrintWriter writer = new PrintWriter(filename);
            int numLines = lines.size();
            for (int i=0; i<numLines; i++)
                writer.println(lines.get(i));
            writer.close();

            return true;
        }
        catch (Exception ex) {
            System.out.println(ex.toString());

            return false;
        }

    }

    public static ArrayList<String> fileToStrArr(String filename) {
        try {
            Scanner s = new Scanner(new File(filename));
            ArrayList<String> list = new ArrayList<String>();
            while (s.hasNextLine()) {
                list.add(s.nextLine());
            }
            s.close();

            return list;
        }
        catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }

    public static void test3() {
        System.out.println("Test 3 called.");

        String token = "query";

        ArrayList<String> part1Arr = fileToStrArr("part1Query");
        ArrayList<String> part2Arr = fileToStrArr("part2Query");

        class Doc {
            public String docno;
            public int freq;
            Doc(String docno, int freq) {
                this.docno = docno;
                this.freq = freq;
            }
            Doc(String line) {
                String temp = "";
                boolean docnoFinished = false;
                for (int i=0; i<line.length(); i++) {
                    if (line.charAt(i) != '\t')
                        temp += line.charAt(i);
                    else {
                        this.docno = temp;
                        docnoFinished = true;
                        temp = "";
                    }
                }
                this.freq = Integer.parseInt(temp);
            }
        }

        List<Doc> part1Docs = new ArrayList<>();
        List<Doc> part2Docs = new ArrayList<>();

        for (int i=0; i<part1Arr.size(); i++)
            part1Docs.add(new Doc(part1Arr.get(i)));
        for (int i=0; i<part2Arr.size(); i++)
            part2Docs.add(new Doc(part2Arr.get(i)));

        Collections.sort(part1Docs, new Comparator<Doc>() {
            @Override
            public int compare(Doc o1, Doc o2) {
                return o1.docno.compareTo(o2.docno);
            }
        });
        Collections.sort(part2Docs, new Comparator<Doc>() {
            @Override
            public int compare(Doc o1, Doc o2) {
                return o1.docno.compareTo(o2.docno);
            }
        });

        List<Doc> notEqual = new ArrayList<>();
        int i = 0;
        int j = 0;
        Doc d1, d2;
        System.out.println("PART 1 ... PART 2");
        while (i < part1Docs.size() && j < part2Docs.size()) {
//            System.out.println("Iteration.");

            d1 = part1Docs.get(i);
            d2 = part2Docs.get(j);

            if (d1.docno.compareTo(d2.docno) > 0) {
                System.out.println("WARNING: (not found)" + d1.docno + "[" + Integer.toString(d1.freq) + "] != " + d2.docno + "[" + Integer.toString(d2.freq) + "]");
                j++;
            }
            else if (d1.docno.compareTo(d2.docno) < 0) {
//                System.out.println("WARNING: " + d1.docno + "[" + Integer.toString(d1.freq) + "] != " + d2.docno + "[" + Integer.toString(d2.freq) + "]");
                i++;
            }
            else {
                if (d1.freq != d2.freq) {
                    System.out.println("WARNING: " + d1.docno + "[" + Integer.toString(d1.freq) + "] != " + d2.docno + "[" + Integer.toString(d2.freq) + "]");
                }
                i++;
                j++;
            }

        }

    }
}
