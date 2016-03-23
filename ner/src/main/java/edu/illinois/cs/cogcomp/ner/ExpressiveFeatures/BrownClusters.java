package edu.illinois.cs.cogcomp.ner.ExpressiveFeatures;

import edu.illinois.cs.cogcomp.ner.IO.InFile;
import edu.illinois.cs.cogcomp.ner.IO.ResourceUtilities;
import edu.illinois.cs.cogcomp.ner.LbjTagger.Data;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.ner.LbjTagger.ParametersForLbjCode;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import gnu.trove.map.hash.THashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;


public class BrownClusters {

    /** the sole instance of this class. */
    private static BrownClusters brownclusters = null;

    /**
     * This method should never be called before init, or the gazetteer will not be initialized.
     * 
     * @return the singleton instance of the Gazetteers class.
     */
    static public BrownClusters get() {
        return brownclusters;
    }

    /** ensures singleton-ness. */
    private BrownClusters() {

    }

    private boolean[] isLowercaseBrownClustersByResource = null;
    private ArrayList<String> resources = null;
    private ArrayList<THashMap<String, String>> wordToPathByResource = null;
    private final int[] prefixLengths = {4, 6, 10, 20};

    public static void init(Vector<String> pathsToClusterFiles, Vector<Integer> thresholds,
            Vector<Boolean> isLowercaseBrownClusters) {
        if (brownclusters != null) {
            return;
        }
        brownclusters = new BrownClusters();
        brownclusters.isLowercaseBrownClustersByResource =
                new boolean[isLowercaseBrownClusters.size()];
        brownclusters.wordToPathByResource = new ArrayList<>();
        brownclusters.resources = new ArrayList<>();
        for (int i = 0; i < pathsToClusterFiles.size(); i++) {
            THashMap<String, String> h = new THashMap<>();
            InFile in =
                    new InFile(ResourceUtilities.loadResource(pathsToClusterFiles.elementAt(i)));
            String line = in.readLine();
            int wordsAdded = 0;
            while (line != null) {
                StringTokenizer st = new StringTokenizer(line);
                String path = st.nextToken();
                String word = st.nextToken();
                int occ = Integer.parseInt(st.nextToken());
                if (occ >= thresholds.elementAt(i)) {
                    h.put(word, path);
                    wordsAdded++;
                }
                line = in.readLine();
            }

            if (ParametersForLbjCode.currentParameters.debug) {
                System.out.println(wordsAdded + " words added");
            }
            brownclusters.wordToPathByResource.add(h);
            brownclusters.isLowercaseBrownClustersByResource[i] =
                    isLowercaseBrownClusters.elementAt(i);
            brownclusters.resources.add(pathsToClusterFiles.elementAt(i));
            in.close();
        }
    }

    /**
     * @return the resource names array.
     */
    final public ArrayList<String> getResources() {
        return resources;
    }

    final public String[] getPrefixes(NEWord w) {
        return getPrefixes(w.form);
    }

    final public String[] getPrefixes(String word) {
        Vector<String> v = new Vector<>();
        for (int j = 0; j < wordToPathByResource.size(); j++) {
            if (isLowercaseBrownClustersByResource[j])
                word = word.toLowerCase();
            THashMap<String, String> wordToPath = wordToPathByResource.get(j);
            if (wordToPath != null && wordToPath.containsKey(word)) {
                String path = wordToPath.get(word);
                v.addElement("resource" + j + ":"
                        + path.substring(0, Math.min(path.length(), prefixLengths[0])));
                for (int i = 1; i < prefixLengths.length; i++)
                    if (prefixLengths[i - 1] < path.length())
                        v.addElement("resource" + j + ":"
                                + path.substring(0, Math.min(path.length(), prefixLengths[i])));
            }
        }
        String[] res = new String[v.size()];
        for (int i = 0; i < v.size(); i++)
            res[i] = v.elementAt(i);
        return res;
    }

    private static void printArr(String[] arr) {
        for (String anArr : arr)
            System.out.print(" " + anArr);
        System.out.println("");
    }

    final public void printOovData(Data data) {
        HashMap<String, Boolean> tokensHash = new HashMap<>();
        HashMap<String, Boolean> tokensHashIC = new HashMap<>();
        ArrayList<LinkedVector> sentences = new ArrayList<>();
        for (int docid = 0; docid < data.documents.size(); docid++)
            for (int sid = 0; sid < data.documents.get(docid).sentences.size(); sid++)
                sentences.add(data.documents.get(docid).sentences.get(sid));
        for (LinkedVector sentence : sentences)
            for (int j = 0; j < sentence.size(); j++) {
                String form = ((NEWord) sentence.get(j)).form;
                tokensHash.put(form, true);
                tokensHashIC.put(form.toLowerCase(), true);
            }
        /*
         * System.out.println("Data statistics:");
         * System.out.println("\t\t- Total tokens with repetitions ="+ totalTokens);
         * System.out.println("\t\t- Total unique tokens  ="+ tokensHash.size());
         * System.out.println("\t\t- Total unique tokens ignore case ="+ tokensHashIC.size());
         */
        for (THashMap<String, String> wordToPath : wordToPathByResource) {
            HashMap<String, Boolean> oovCaseSensitiveHash = new HashMap<>();
            HashMap<String, Boolean> oovAfterLowercasingHash = new HashMap<>();
            for (LinkedVector sentence : sentences) {
                for (int j = 0; j < sentence.size(); j++) {
                    String form = ((NEWord) sentence.get(j)).form;
                    if (!wordToPath.containsKey(form)) {
                        oovCaseSensitiveHash.put(form, true);
                    }
                    if ((!wordToPath.containsKey(form))
                            && (!wordToPath.containsKey(form.toLowerCase()))) {
                        oovAfterLowercasingHash.put(form.toLowerCase(), true);
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        /*
         * Vector<String> resources=new Vector<>();
         * resources.addElement("Data/BrownHierarchicalWordClusters/brownBllipClusters");
         * Vector<Integer> thres=new Vector<>(); thres.addElement(5); Vector<Boolean> lowercase=new
         * Vector<>(); lowercase.addElement(false); init(resources,thres,lowercase);
         * System.out.println("finance "); printArr(getPrefixes(new NEWord(new
         * Word("finance"),null,null))); System.out.println("help"); printArr(getPrefixes(new
         * NEWord(new Word("help"),null,null))); System.out.println("resque ");
         * printArr(getPrefixes(new NEWord(new Word("resque"),null,null)));
         * System.out.println("assist "); printArr(getPrefixes(new NEWord(new
         * Word("assist"),null,null))); System.out.println("assistance "); printArr(getPrefixes(new
         * NEWord(new Word("assistance"),null,null))); System.out.println("guidance ");
         * printArr(getPrefixes(new NEWord(new Word("guidance"),null,null)));
         */
    }
}
