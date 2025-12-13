package com.searchengine.indexer;

/**
 * Porter Stemmer implementation for English word normalization.
 *
 * This is a Java implementation of the Porter Stemming Algorithm.
 * Based on the original work by Martin Porter.
 *
 * The stemmer reduces words to their root form:
 * - "running" → "run"
 * - "studies" → "studi"
 * - "better" → "better"
 */
public class Stemmer {

    private char[] b;
    private int i;     // offset into b
    private int j;     // offset to end of string

    public Stemmer() {
        b = new char[50];
        i = 0;
        j = 0;
    }

    /**
     * Stem a word and return the stemmed version.
     *
     * @param word The word to stem
     * @return The stemmed word
     */
    public String stem(String word) {
        if (word == null || word.length() <= 2) {
            return word;
        }

        // Reset the stemmer
        i = 0;
        j = 0;

        // Add the word to the internal buffer
        for (char c : word.toCharArray()) {
            add(c);
        }

        // Perform stemming
        stem();

        return toString();
    }

    /**
     * Add a character to the word being stemmed.
     */
    private void add(char ch) {
        if (i == b.length) {
            char[] new_b = new char[i + 50];
            System.arraycopy(b, 0, new_b, 0, i);
            b = new_b;
        }
        b[i++] = ch;
    }

    /**
     * Convert the stemmed word to a string.
     */
    @Override
    public String toString() {
        return new String(b, 0, j);
    }

    /**
     * Check if b[i] is a consonant.
     */
    private boolean cons(int i) {
        switch (b[i]) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return false;
            case 'y':
                return (i == 0) || !cons(i - 1);
            default:
                return true;
        }
    }

    /**
     * Measure the number of consonant sequences between 0 and j.
     *
     * A consonant sequence is defined as a sequence of consonants followed by vowels.
     * For example:
     * - "tr" has measure 0
     * - "tree" has measure 1
     * - "trees" has measure 1
     * - "trouble" has measure 2
     */
    private int m() {
        int n = 0;
        int i = 0;
        while (true) {
            if (i > j) return n;
            if (!cons(i)) break;
            i++;
        }
        i++;
        while (true) {
            while (true) {
                if (i > j) return n;
                if (cons(i)) break;
                i++;
            }
            i++;
            n++;
            while (true) {
                if (i > j) return n;
                if (!cons(i)) break;
                i++;
            }
            i++;
        }
    }

    /**
     * Check if 0...j contains a vowel.
     */
    private boolean vowelinstem() {
        for (int i = 0; i <= j; i++) {
            if (!cons(i)) return true;
        }
        return false;
    }

    /**
     * Check if j and j-1 contain a double consonant.
     */
    private boolean doublec(int j) {
        if (j < 1) return false;
        if (b[j] != b[j - 1]) return false;
        return cons(j);
    }

    /**
     * Check if i-2,i-1,i has the form consonant-vowel-consonant
     * and also if the second c is not w,x or y.
     */
    private boolean cvc(int i) {
        if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false;
        int ch = b[i];
        return ch != 'w' && ch != 'x' && ch != 'y';
    }

    /**
     * Check if the stem ends with a given string.
     */
    private boolean ends(String s) {
        int l = s.length();
        int o = j - l + 1;
        if (o < 0) return false;
        for (int i = 0; i < l; i++) {
            if (b[o + i] != s.charAt(i)) return false;
        }
        j = j - l;
        return true;
    }

    /**
     * Set the string to s if the measure is greater than 0.
     */
    private void setto(String s) {
        int l = s.length();
        int o = j + 1;
        for (int i = 0; i < l; i++) {
            b[o + i] = s.charAt(i);
        }
        j = j + l;
    }

    /**
     * Replace the ending with s if measure > 0.
     */
    private void r(String s) {
        if (m() > 0) setto(s);
    }

    /**
     * Step 1a: plurals and -ed or -ing.
     *
     * Examples:
     * - caresses → caress
     * - ponies → poni
     * - ties → ti
     * - cats → cat
     */
    private void step1ab() {
        if (b[j] == 's') {
            if (ends("sses")) j -= 2;
            else if (ends("ies")) setto("i");
            else if (b[j - 1] != 's') j--;
        }
        if (ends("eed")) {
            if (m() > 0) j--;
        } else if ((ends("ed") || ends("ing")) && vowelinstem()) {
            // Stem remains as-is after removing -ed/-ing
            if (ends("at")) setto("ate");
            else if (ends("bl")) setto("ble");
            else if (ends("iz")) setto("ize");
            else if (doublec(j)) {
                j--;
                int ch = b[j];
                if (ch == 'l' || ch == 's' || ch == 'z') j++;
            } else if (m() == 1 && cvc(j)) setto("e");
        }
    }

    /**
     * Step 1c: turn terminal y to i when there is another vowel in the stem.
     */
    private void step1c() {
        if (ends("y") && vowelinstem()) b[j] = 'i';
    }

    /**
     * Step 2: map double suffices to single ones.
     *
     * Examples:
     * - relational → relate
     * - conditional → condition
     */
    private void step2() {
        if (j < 1) return;
        switch (b[j - 1]) {
            case 'a':
                if (ends("ational")) { r("ate"); break; }
                if (ends("tional")) { r("tion"); break; }
                break;
            case 'c':
                if (ends("enci")) { r("ence"); break; }
                if (ends("anci")) { r("ance"); break; }
                break;
            case 'e':
                if (ends("izer")) { r("ize"); break; }
                break;
            case 'l':
                if (ends("bli")) { r("ble"); break; }
                if (ends("alli")) { r("al"); break; }
                if (ends("entli")) { r("ent"); break; }
                if (ends("eli")) { r("e"); break; }
                if (ends("ousli")) { r("ous"); break; }
                break;
            case 'o':
                if (ends("ization")) { r("ize"); break; }
                if (ends("ation")) { r("ate"); break; }
                if (ends("ator")) { r("ate"); break; }
                break;
            case 's':
                if (ends("alism")) { r("al"); break; }
                if (ends("iveness")) { r("ive"); break; }
                if (ends("fulness")) { r("ful"); break; }
                if (ends("ousness")) { r("ous"); break; }
                break;
            case 't':
                if (ends("aliti")) { r("al"); break; }
                if (ends("iviti")) { r("ive"); break; }
                if (ends("biliti")) { r("ble"); break; }
                break;
            case 'g':
                if (ends("logi")) { r("log"); break; }
                break;
        }
    }

    /**
     * Step 3: deals with -ic-, -full, -ness etc.
     */
    private void step3() {
        if (j < 0) return;
        switch (b[j]) {
            case 'e':
                if (ends("icate")) { r("ic"); break; }
                if (ends("ative")) { r(""); break; }
                if (ends("alize")) { r("al"); break; }
                break;
            case 'i':
                if (ends("iciti")) { r("ic"); break; }
                break;
            case 'l':
                if (ends("ical")) { r("ic"); break; }
                if (ends("ful")) { r(""); break; }
                break;
            case 's':
                if (ends("ness")) { r(""); break; }
                break;
        }
    }

    /**
     * Step 4: takes off -ant, -ence etc., in context <c>vcvc<v>.
     */
    private void step4() {
        if (j < 1) return;
        switch (b[j - 1]) {
            case 'a':
                if (ends("al")) break;
                return;
            case 'c':
                if (ends("ance")) break;
                if (ends("ence")) break;
                return;
            case 'e':
                if (ends("er")) break;
                return;
            case 'i':
                if (ends("ic")) break;
                return;
            case 'l':
                if (ends("able")) break;
                if (ends("ible")) break;
                return;
            case 'n':
                if (ends("ant")) break;
                if (ends("ement")) break;
                if (ends("ment")) break;
                if (ends("ent")) break;
                return;
            case 'o':
                if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) break;
                if (ends("ou")) break;
                return;
            case 's':
                if (ends("ism")) break;
                return;
            case 't':
                if (ends("ate")) break;
                if (ends("iti")) break;
                return;
            case 'u':
                if (ends("ous")) break;
                return;
            case 'v':
                if (ends("ive")) break;
                return;
            case 'z':
                if (ends("ize")) break;
                return;
            default:
                return;
        }
        // If m() > 1, the stem remains as-is (already set by ends())
    }

    /**
     * Step 5: remove a final -e if m() > 1.
     */
    private void step5() {
        j = i - 1;
        if (j < 0) return;
        if (b[j] == 'e') {
            int a = m();
            if (a > 1 || a == 1 && !cvc(j - 1)) j--;
        }
        if (j >= 0 && b[j] == 'l' && doublec(j) && m() > 1) j--;
    }

    /**
     * Perform the complete stemming algorithm.
     */
    private void stem() {
        j = i - 1;
        if (j > 1) {
            step1ab();
            step1c();
            step2();
            step3();
            step4();
            step5();
        }
    }
}
