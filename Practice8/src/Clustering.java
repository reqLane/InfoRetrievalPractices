import com.kursx.parser.fb2.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Clustering {
    private static final int RESULT_SIZE = 3;
    private static int MAX_CLUSTER_SIZE;

    //documents' ids
    private final ArrayList<FictionBook> documents;

    //inverted index (tf(t,d) + idf(t))
    private HashMap<String, HashMap<Integer, Integer>> termFreq;
    //term ids (dictionary)
    private HashMap<String, Integer> termIds;
    private int freeId;

    //documents' vectors
    private ArrayList<DocumentVector> vectors;
    //leaders documents' ids
    private TreeSet<Integer> leaders;
    //clusters (leader -> followers)
    private HashMap<Integer, TreeSet<Integer>> clusters;


    public Clustering() {
        termFreq = new HashMap<>();
        termIds = new HashMap<>();
        freeId = 0;
        documents = new ArrayList<>();
        vectors = new ArrayList<>();
        leaders = new TreeSet<>();
        clusters = new HashMap<>();
    }

    public void addDocument(File file) throws IllegalArgumentException {
        if(!file.exists() || !file.getName().endsWith(".fb2"))
            throw new IllegalArgumentException("Incorrect file");

        FictionBook document;
        try {
            document = new FictionBook(file);
            documents.add(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void build() {
        //reset
        MAX_CLUSTER_SIZE = (int)Math.round(Math.sqrt(documents.size())) + 1;
        termFreq = new HashMap<>();
        termIds = new HashMap<>();
        freeId = 0;
        vectors = new ArrayList<>();
        leaders = new TreeSet<>();
        clusters = new HashMap<>();

        //process
        for (int docId = 0; docId < documents.size(); docId++) {
            processDocument(documents.get(docId), docId);
        }

        //build clusters
        buildDocumentVectors();
        buildClusters();
    }

    private void processDocument(FictionBook doc, int docId) {
        if(doc == null) return;

        //Book title (one)
        processText(doc.getTitle(), docId);

        //Sections (many)
        for (Section section : doc.getBody().getSections()) {
            processSection(section, docId);
        }
    }

    private void processSection(Section section, int docId) {
        if(section == null) return;

        //Annotation (one)
        processAnnotation(section.getAnnotation(), docId);

        //Title paragraphs (many) AS Elements
        for (Title title : section.getTitles()) {
            for (P paragraph : title.getParagraphs()) {
                processElement(paragraph, docId);
            }
        }

        //Elements (many)
        for (Element element : section.getElements()) {
            processElement(element, docId);
        }

        //Sections (many)
        for (Section sect : section.getSections()) {
            processSection(sect, docId);
        }
    }

    private void processAnnotation(Annotation annotation, int docId) {
        if(annotation == null) return;

        //Elements (many)
        for (Element element : annotation.getAnnotations()) {
            processElement(element, docId);
        }
    }

    private void processElement(Element element, int docId) {
        if(element == null) return;

        processText(element.getText(), docId);
    }

    private void processText(String text, int docId) {
        if(text == null) return;

        String[] terms = text.split("[^A-Za-z]+");
        for (String term : terms) {
            term = term.trim().toLowerCase();
            if(term.length() != 0 && term.length() <= 20) {
                //1
                if(!termIds.containsKey(term)) {
                    termIds.put(term, freeId++);
                }

                //2
                if(!termFreq.containsKey(term))
                    termFreq.put(term, new HashMap<>());

                Integer tf = termFreq.get(term).get(docId);
                termFreq.get(term).put(docId, tf == null ? 1 : tf + 1);
            }
        }
    }

    void buildDocumentVectors() {
        for(int docId = 0; docId < documents.size(); docId++) {
            DocumentVector vector = new DocumentVector();

            for (String term : termFreq.keySet()) {
                if(termFreq.get(term).containsKey(docId)) {
                    double termWeight = (1 + Math.log(termFreq.get(term).get(docId)))
                            * (1 + Math.log((double)documents.size() / termFreq.get(term).size()));
                    vector.put(termIds.get(term), termWeight);
                }
            }

            vector.computeLength();
            vectors.add(vector);
        }
    }

    void buildClusters() {
        //select leaders
        int leadersCount = (int)Math.round(Math.sqrt(documents.size()));

        ArrayList<Integer> unusedDocIds = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            unusedDocIds.add(i);
        }

        while(leaders.size() < leadersCount) {
            int randomUnusedIndex = (int)(Math.random() * unusedDocIds.size());
            int leaderId = unusedDocIds.remove(randomUnusedIndex);
            leaders.add(leaderId);
            TreeSet<Integer> cluster = new TreeSet<>();
            cluster.add(leaderId);
            clusters.put(leaderId, cluster);
        }

        //build clusters with all unused documents
        for (Integer followerId : unusedDocIds) {
            TreeMap<Double, Integer> mostSimilarLeaders = new TreeMap<>();

            DocumentVector followerVector = vectors.get(followerId);
            for (Integer leaderId : leaders) {
                mostSimilarLeaders.put(followerVector.similarity(vectors.get(leaderId)), leaderId);
            }

            for (Double similarity : mostSimilarLeaders.keySet()) {
                TreeSet<Integer> cluster = clusters.get(mostSimilarLeaders.get(similarity));
                if(cluster.size() < MAX_CLUSTER_SIZE) {
                    cluster.add(followerId);
                    break;
                }
            }
        }
    }

    public ArrayList<String> findWithQuery(String query) {
        query = query.trim().toLowerCase();
        if(!query.matches("[a-z]+(\\s+[a-z]+)*"))
            return new ArrayList<>();

        String[] queryTerms = query.split("\\s+");

        TreeMap<Double, TreeSet<Integer>> mostRelevantLeaders = new TreeMap<>();
        for (Integer leaderId : leaders) {
            double leaderRelevance = getDocumentRelevance(queryTerms, leaderId);

            if(!mostRelevantLeaders.containsKey(leaderRelevance))
                mostRelevantLeaders.put(leaderRelevance, new TreeSet<>());

            mostRelevantLeaders.get(leaderRelevance).add(leaderId);
        }

        TreeMap<Float, TreeSet<Integer>> resultIds = new TreeMap<>();
        int resultsCount = 0;
        while(resultsCount < RESULT_SIZE) {
            Double firstKey = mostRelevantLeaders.firstKey();
            if(mostRelevantLeaders.get(firstKey).isEmpty()) mostRelevantLeaders.remove(firstKey);
            if(mostRelevantLeaders.isEmpty()) break;

            TreeSet<Integer> firstLeadersSet = mostRelevantLeaders.get(mostRelevantLeaders.firstKey());
            Integer firstCandidateId = firstLeadersSet.first();
            TreeSet<Integer> cluster = clusters.get(firstCandidateId);
            firstLeadersSet.remove(firstCandidateId);

            TreeMap<Double, TreeSet<Integer>> mostRelevantDocuments = new TreeMap<>();
            for (Integer documentId : cluster) {
                double documentRelevance = getDocumentRelevance(queryTerms, documentId);

                if(!mostRelevantDocuments.containsKey(documentRelevance))
                    mostRelevantDocuments.put(documentRelevance, new TreeSet<>());

                mostRelevantDocuments.get(documentRelevance).add(documentId);
            }
            for (Double documentRelevance : mostRelevantDocuments.keySet()) {
                for (Integer documentId : mostRelevantDocuments.get(documentRelevance)) {
                    float resultRelevance = (float)documentRelevance.doubleValue();

                    if(!resultIds.containsKey(resultRelevance))
                        resultIds.put(resultRelevance, new TreeSet<>());

                    resultIds.get(resultRelevance).add(documentId);
                    resultsCount++;

                    if(resultsCount >= RESULT_SIZE)
                        break;
                }
                if(resultsCount >= RESULT_SIZE)
                    break;
            }
        }

        ArrayList<String> result = new ArrayList<>();
        for (Float resultRelevance : resultIds.keySet()) {
            for (Integer resultId : resultIds.get(resultRelevance)) {
                result.add(0, resultId + ". " + documents.get(resultId).getTitle() + " (relevance = " + resultRelevance + ")");
            }
        }

        return result;
    }

    private double getDocumentRelevance(String[] queryTerms, Integer documentId) {
        double documentRelevance = 0;
        for (String queryTerm : queryTerms) {
            Integer queryTermId = termIds.get(queryTerm);
            if(queryTermId != null) {
                Double queryTermWeight = vectors.get(documentId).getTermWight(queryTermId);
                if(queryTermWeight != null) {
                    documentRelevance += queryTermWeight;
                }
            }
        }
        return documentRelevance;
    }

    public void saveToFile(File file) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Integer leaderId : clusters.keySet()) {
            sb.append("-----\n")
                    .append("(Leader) ")
                    .append(documents.get(leaderId).getTitle())
                    .append("\n");

            for (Integer followerId : clusters.get(leaderId))
                sb.append(followerId)
                        .append(". ")
                        .append(documents.get(followerId).getTitle())
                        .append("\n");
        }

        return sb.toString();
    }
}

class DocumentVector {
    private final HashMap<Integer, Double> vector;
    private double euclidLength;

    DocumentVector() {
        vector = new HashMap<>();
    }

    double similarity(DocumentVector other) {
        double res = 0;
        for (Integer termId : vector.keySet()) {
            if(other.containsTerm(termId)) {
                res += getTermWight(termId) * other.getTermWight(termId);
            }
        }
        res /= (getEuclidLength() * other.getEuclidLength());

        return res;
    }

    void put(int termId, double termWeight) {
        vector.put(termId, termWeight);
    }

    void computeLength() {
        euclidLength = 0;
        for (Double termWeight : vector.values()) {
            euclidLength += Math.pow(termWeight, 2);
        }
        euclidLength = Math.sqrt(euclidLength);
    }

    boolean containsTerm(int termId) {
        return vector.containsKey(termId);
    }

    Double getTermWight(int termId) {
        if(!containsTerm(termId))
            return null;

        return vector.get(termId);
    }

    double getEuclidLength() {
        return euclidLength;
    }
}
