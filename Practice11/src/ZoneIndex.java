import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class ZoneIndex {
    private static final float TITLE_WEIGHT = 0.8f;
    private static final float BODY_WEIGHT = 0.2f;
    private static final int RESULT_SIZE = 10;

    HashMap<String, HashMap<Integer, TermWeight>> zoneIndex;
    ArrayList<FictionBook> documents;

    public ZoneIndex() {
        zoneIndex = new HashMap<>();
        documents = new ArrayList<>();
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
        zoneIndex = new HashMap<>();

        for (int docId = 0; docId < documents.size(); docId++) {
            processDocument(documents.get(docId), docId);
        }
    }

    private void processDocument(FictionBook doc, int docId) {
        if(doc == null) return;

        //Book title (one)
        processText(doc.getTitle(), docId, false);

        //Sections (many)
        for (String text : doc.getBody()) {
            processText(text, docId, true);
        }
    }

    private void processText(String text, int docId, boolean bodyText) {
        if(text == null) return;

        String[] terms = text.split("[^A-Za-z]+");
        for (String term : terms) {
            term = term.trim().toLowerCase();
            if(term.length() != 0 && term.length() <= 20) {
                if(!zoneIndex.containsKey(term))
                    zoneIndex.put(term, new HashMap<>());

                HashMap<Integer, TermWeight> termIndex = zoneIndex.get(term);
                if(!termIndex.containsKey(docId)) termIndex.put(docId, new TermWeight());

                if(bodyText) zoneIndex.get(term).get(docId).setInBody(true);
                else         zoneIndex.get(term).get(docId).setInTitle(true);
            }
        }
    }

    public ArrayList<String> findWithQuery(String query) {
        query = query.trim().toLowerCase();
        if(!query.matches("[a-zA-Z]+(\\s+[a-zA-Z]+)*"))
            return new ArrayList<>();

        String[] queryTerms = query.split("\\s+");
        TreeMap<Float, TreeSet<String>> resultList = new TreeMap<>();

        for (int docId = 0; docId < documents.size(); docId++) {
            float docRelevance = 0f;
            for (String term : queryTerms) {
                if(!zoneIndex.containsKey(term)) continue;

                TermWeight termWeight = zoneIndex.get(term).get(docId);
                if(termWeight != null)
                    docRelevance += ((termWeight.isInTitle() ? TITLE_WEIGHT : 0) + (termWeight.isInBody() ? BODY_WEIGHT : 0));
            }
            if(docRelevance != 0f) {
                if(!resultList.containsKey(docRelevance))
                    resultList.put(docRelevance, new TreeSet<>());

                resultList.get(docRelevance).add(documents.get(docId).getTitle());
            }
        }

        ArrayList<String> result = new ArrayList<>();
        for (Float docRelevance : resultList.descendingKeySet()) {
            for (String document : resultList.get(docRelevance)) {
                if(result.size() == RESULT_SIZE) break;
                result.add(document);
            }
            if(result.size() == RESULT_SIZE) break;
        }

        return result;
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

        for (String term : zoneIndex.keySet()) {
            sb.append(term);
            for (Integer docId : zoneIndex.get(term).keySet()) {
                TermWeight termWeight = zoneIndex.get(term).get(docId);
                float weight = (termWeight.isInTitle() ? TITLE_WEIGHT : 0) + (termWeight.isInBody() ? BODY_WEIGHT : 0);
                sb.append('|').append(docId).append(' ').append(weight);
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}

class TermWeight {
    private boolean inTitle;
    private boolean inBody;

    public TermWeight() {

    }

    public boolean isInTitle() {
        return inTitle;
    }

    public void setInTitle(boolean inTitle) {
        this.inTitle = inTitle;
    }

    public boolean isInBody() {
        return inBody;
    }

    public void setInBody(boolean inBody) {
        this.inBody = inBody;
    }
}
