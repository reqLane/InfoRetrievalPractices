import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Clustering clusterIndex = new Clustering();
        File dir = new File("src/documents");

        try {
            for (File file : Objects.requireNonNull(dir.listFiles()))
                clusterIndex.addDocument(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        clusterIndex.build();
        clusterIndex.saveToFile(new File("src/output/clusters.txt"));

        System.out.println("Clustered Index Testing\n-----");
        Scanner sc = new Scanner(System.in);
        int ans;
        do {
            System.out.print("Query: ");
            ArrayList<String> documents = clusterIndex.findWithQuery(sc.nextLine());
            for (String document : documents) {
                System.out.println(document);
            }

            System.out.print("-----\nRepeat?(1-yes;0-no): ");
            ans = Integer.parseInt(sc.nextLine());
        } while(ans != 0);
    }
}
