import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Clustering clusterIndex = new Clustering();
        File documentsFile = new File("src/cranfield/cran.all.1400");
        File queriesFile = new File("src/cranfield/cran.qry");
        File relevanceFile = new File("src/cranfield/cranqrel");

        clusterIndex.setDataCranfield(documentsFile, queriesFile, relevanceFile);

        clusterIndex.build();
        clusterIndex.saveToFile(new File("src/output/clusters.txt"));

        System.out.println(clusterIndex.getFDegree(100));
    }
}
