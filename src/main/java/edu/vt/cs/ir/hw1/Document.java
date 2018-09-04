package edu.vt.cs.ir.hw1;

import java.util.ArrayList;

public class Document {
    private String docno;
    private String text;
    private int textLen;
    private ArrayList<String> tokens;
    private int numTokens;
    private static int UPPER_CASE_DIFF = 32;

    Document(String docno, String text) {
        this.docno = docno;
        this.text = text;
        this.textLen = text.length();

        this.tokenize();
    }

    private void tokenize() {
        String newToken = "";
        char currChar = '\0';
        tokens = new ArrayList<String>();

        /* go through every character, and tokenize */
        for (int i=0; i<textLen; i++) {
            currChar = text.charAt(i);
            if ((currChar >= 'a' && currChar <= 'z') || (currChar >= 'A' && currChar <= 'Z') || (currChar >= '0' && currChar <= '9')) {
                /* ensure lower case */
                if (currChar >= 'A' && currChar <= 'Z')
                    currChar += UPPER_CASE_DIFF;

                newToken += currChar;
            }
            else {
                if (newToken.length() > 0) {
                    tokens.add(newToken);
                    newToken = "";
                }
            }
        }

        /* check last token */
        if (newToken.length() > 0)
            tokens.add(newToken);

        numTokens = tokens.size();
    }
    public void printTokens() {
        String printString = "";
        for (int i=0; i<numTokens; i++) {
            printString += tokens.get(i) + " ";
        }
        System.out.println(printString);
    }
    public String getTokenAtIndex(int i) {
        return this.tokens.get(i);
    }
    public String getTokenAtIndex(Integer i) {
        return this.tokens.get(i);
    }
    public int getNumTokens() {
        return this.numTokens;
    }
    public boolean contains(String token) {
        for (int i=0; i<numTokens; i++) {
            if (tokens.get(i) == token)
                return true;
        }
        return false;
    }

    public String getText() {
        return this.text;
    }
    public int getTextFieldLen() {
        return this.numTokens;
    }
    public String getDocno() { return  this.docno; }


}
