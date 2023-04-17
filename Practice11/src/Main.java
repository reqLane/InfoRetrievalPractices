import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ZoneIndex zoneIndex = new ZoneIndex();
        File dir = new File("src/documents");

        try {
            for (File file : Objects.requireNonNull(dir.listFiles()))
                zoneIndex.addDocument(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        zoneIndex.build();

        System.out.println("Zone Index Testing\n-----");
        Scanner sc = new Scanner(System.in);
        int ans;
        do {
            System.out.print("Query: ");
            ArrayList<String> documents = zoneIndex.findWithQuery(sc.nextLine());
            for (int i = 0; i < documents.size(); i++) {
                System.out.println((i + 1) + ". " + documents.get(i));
            }

            System.out.print("-----\nRepeat?(1-yes;0-no): ");
            ans = Integer.parseInt(sc.nextLine());
        } while(ans != 0);
    }
}
