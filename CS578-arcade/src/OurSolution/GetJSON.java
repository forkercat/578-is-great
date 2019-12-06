/**
 * @author Junhao Wang
 * @date 12/03/2019
 */
package OurSolution;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetJSON {

  public static void main(String[] args) throws IOException {
    depend();
    acdc();
    arc();
  }


  private static void depend() throws IOException {
    String file = "../tomcat/output/" + "ACDC/" + "8.5.47_deps.rsf";

    FileReader fr = new FileReader(file);

    BufferedReader br = new BufferedReader(fr);
    String line = "";
    String[] arr = null;

    Map<String, List<String>> map = new HashMap<>(); // <cluster name, list>

    while ((line = br.readLine()) != null) {
      arr = line.split("\\s+");
      String name = arr[1];
      String comp = arr[2];

      List<String> compList = map.getOrDefault(name, new ArrayList<>());
      compList.add(comp);

      map.put(name, compList);
    }

    JSONArray clusterList = new JSONArray();

    for (String name : map.keySet()) {
      List<String> compList = map.get(name);
      JSONObject cluster = new JSONObject();
      cluster.put("name", name);
      cluster.put("imports", compList);

      clusterList.add(cluster);
    }

    // write to output.json
    Writer write = new FileWriter("output/output_depend.json");
    write.write(clusterList.toString());
    write.flush();
    write.close();

    br.close();
    fr.close();
  }


  private static void acdc() throws IOException {
    String file = "/Users/chuck/Desktop/578-is-great/tomcat/output/" + "ACDC/" + "8.5.47_acdc_clustered.rsf";

    FileReader fr = new FileReader(file);

    BufferedReader br = new BufferedReader(fr);
    String line = "";
    String[] arr = null;

    Map<String, List<String>> map = new HashMap<>(); // <cluster name, list>

    while ((line = br.readLine()) != null) {
      arr = line.split("\\s+");
      String name = arr[1];
      String comp = arr[2];

      List<String> compList = map.getOrDefault(name, new ArrayList<>());
      compList.add(comp);

      map.put(name, compList);
    }

    JSONArray clusterList = new JSONArray();

    for (String name : map.keySet()) {
      List<String> compList = map.get(name);
      JSONObject cluster = new JSONObject();
      cluster.put("name", name);
      cluster.put("imports", compList);

      clusterList.add(cluster);
    }

    // write to output.json
    Writer write = new FileWriter("output/output_acdc.json");
    write.write(clusterList.toString());
    write.flush();
    write.close();

    br.close();
    fr.close();
  }


  private static void arc() throws IOException {

    String file = "/Users/chuck/Desktop/578-is-great/tomcat/output/" + "ARC/" + "8.5.47_504_topics_485_arc_clusters.rsf";

    FileReader fr = new FileReader(file);

    BufferedReader br = new BufferedReader(fr);
    String line = "";
    String[] arr = null;

    Map<String, List<String>> map = new HashMap<>(); // <cluster name, list>

    while ((line = br.readLine()) != null) {
      arr = line.split("\\s+");
      String num = arr[1]; // it is a number
      String comp = arr[2];
      String name = num + " - " + comp.split("\\$")[0];

      List<String> compList = map.getOrDefault(name, new ArrayList<>());
      compList.add(comp);

      map.put(name, compList);
    }

    JSONArray clusterList = new JSONArray();

    for (String name : map.keySet()) {
      List<String> compList = map.get(name);
      JSONObject cluster = new JSONObject();
      cluster.put("name", name);
      cluster.put("imports", compList);

      clusterList.add(cluster);
    }

    // write to output.json
    Writer write = new FileWriter("output/output_arc.json");
    write.write(clusterList.toString());
    write.flush();
    write.close();

    br.close();
    fr.close();
  }
}





