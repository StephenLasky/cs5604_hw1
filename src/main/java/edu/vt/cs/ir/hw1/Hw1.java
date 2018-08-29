package edu.vt.cs.ir.hw1;

/*
* This will act as the main method for the Java class.
*
*
 */


//import java.io.FileReader;
//import java.io.BufferedReader;
import java.io.*;
import java.util.ArrayList;

public class Hw1 {
    public static String FILEPATH = "/Users/stephenlasky/Documents/cs5604/CS5604_HW1_b/CS5604_HW1/acm_corpus";

    public static void main(String[] args) {
        System.out.println("Hello Stephen Lasky.");

        part1();

    }

    private static ArrayList<String> input = new ArrayList<String>();
    /*
     * Please Note: File opening code is courtesy of https://www.caveofprogramming.com/java/java-file-reading-and-writing-files-in-java.html
     */
    private static void openFile() {
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
                input.add(line);
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
    }

    public static void part1() {
        System.out.println("Part 1 begin.");

        System.out.println("Reading file.");
        openFile();
        System.out.println("End reading file.");

    }



}
