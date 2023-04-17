import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FictionBook {

    private String title;

    private final ArrayList<String> body;

    public FictionBook(File file) {

        body = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {

            retrieveTitle(br);
            retrieveBody(br);

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void retrieveTitle(BufferedReader br) throws IOException {

        String s = br.readLine();
        while(!s.startsWith("<book-title>")) {
            s = br.readLine().trim();
        }

        StringBuilder sb = new StringBuilder();

        while(!s.contains("</book-title>")) {
            s = s.trim();

            String[] terms = s.split("[^A-Za-z]+");
            for (String term : terms) {
                sb.append(term).append(" ");
            }

            s = br.readLine().trim();
        }

        int endIndex = s.indexOf("</book-title>");
        if(s.contains("<book-title>")) {
            int startIndex = s.indexOf("<book-title>") + "<book-title>".length();
            s = s.substring(startIndex, endIndex);
        }

        String[] terms = s.split("[^A-Za-z]+");
        for (String term : terms) {
            sb.append(term).append(" ");
        }

        title = sb.toString().substring(0, sb.length() - 1);
    }

    private void retrieveBody(BufferedReader br) throws IOException {
        String s = br.readLine().trim();

        while(s != null) {
            s = s.trim();
            if(s.startsWith("<p>")) {
                addParagraph(s, "p");
            }
            else if(s.startsWith("<subtitle>")) {
                addParagraph(s, "subtitle");
            }
            else if(s.startsWith("<v>")) {
                addParagraph(s, "v");
            }

            s = br.readLine();
        }
    }

    private void addParagraph(String text, String tag) {
        String openTag = '<' + tag + '>';
        String closeTag = "</" + tag + '>';
        int startIndex = text.indexOf(openTag) + openTag.length();
        int endIndex = text.indexOf(closeTag);
        text = text.substring(startIndex, endIndex)
                .replaceAll("<emphasis>", "")
                .replaceAll("</emphasis>", "")
                .replaceAll("<strong>", "")
                .replaceAll("</strong>", "");

        body.add(text);
    }

    public String getTitle() {
        return title;
    }

    public ArrayList<String> getBody() {
        return new ArrayList<>(body);
    }
}
