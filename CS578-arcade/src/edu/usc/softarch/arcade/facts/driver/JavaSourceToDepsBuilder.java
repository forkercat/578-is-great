package edu.usc.softarch.arcade.facts.driver;

import OurSolution.Tomdog;
import classycle.Analyser;
import classycle.ClassAttributes;
import classycle.graph.AtomicVertex;
import edu.usc.softarch.arcade.clustering.FastFeatureVectors;
import edu.usc.softarch.arcade.clustering.FeatureVectorMap;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.functiongraph.TypedEdgeGraph;
import edu.usc.softarch.arcade.util.FileUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JavaSourceToDepsBuilder implements SourceToDepsBuilder {

  static Logger logger = Logger.getLogger(JavaSourceToDepsBuilder.class);

  public Set<Pair<String, String>> edges;
  public static FastFeatureVectors ffVecs = null;
  public int numSourceEntities = 0;

  @Override
  public Set<Pair<String, String>> getEdges() {
    return this.edges;
  }

  @Override
  public int getNumSourceEntities() {
    return this.numSourceEntities;
  }

  public static void main(String[] args) throws IOException {
    (new JavaSourceToDepsBuilder()).build(args);
  }

  public void build(String[] args) throws IOException,
          FileNotFoundException {
    PropertyConfigurator.configure(Config.getLoggingConfigFilename());

    String[] inputClasses = {FileUtil.tildeExpandPath(args[0])};
    String depsRsfFilename = FileUtil.tildeExpandPath(args[1]); // output file

    /** Analyzer */
    Analyser analyzer = new Analyser(inputClasses);
    analyzer.readAndAnalyse(false);
    // analyzer.printRaw(new PrintWriter(System.out));

    /** Graph */
    PrintStream out = new PrintStream(depsRsfFilename);
    PrintWriter writer = new PrintWriter(out);
    AtomicVertex[] graph = analyzer.getClassGraph();  // <--------- key

    /** Edges */
    edges = new LinkedHashSet<Pair<String, String>>(); /** collect edges */
    for (int i = 0; i < graph.length; i++) { /** for each vertex */
      AtomicVertex vertex = graph[i];
      ClassAttributes sourceAttributes = (ClassAttributes) vertex.getAttributes();
      // writer.println(sourceAttributes.getType() +  " " + sourceAttributes.getName());

      /** consider its outgoing edges */
      for (int j = 0, n = vertex.getNumberOfOutgoingArcs(); j < n; j++) {
        ClassAttributes targetAttributes = (ClassAttributes) vertex.getHeadVertex(j).getAttributes();
        // writer.println("    " + targetAttributes.getType() + " " + targetAttributes.getName());
        Pair<String, String> edge = new ImmutablePair<String, String>(sourceAttributes.getName(), targetAttributes.getName());
        edges.add(edge);
      }
    }

    /** Our Solution */
    if (true) {
      runOurSolution();
    }

    // if (true) return; // for debugging our solution

    /** Output */
    for (Pair<String, String> edge : edges) { /** each edge is a dependency */
      writer.println("depends " + edge.getLeft() + " " + edge.getRight());
    }
    writer.close();

    Set<String> sources = new HashSet<String>();
    for (Pair<String, String> edge : edges) {
      sources.add(edge.getLeft());
    }
    numSourceEntities = sources.size();

    TypedEdgeGraph typedEdgeGraph = new TypedEdgeGraph();
    for (Pair<String, String> edge : edges) {
      typedEdgeGraph.addEdge("depends", edge.getLeft(), edge.getRight());
    }

    FeatureVectorMap fvMap = new FeatureVectorMap(typedEdgeGraph);
    ffVecs = fvMap.convertToFastFeatureVectors();
  }

  @Override
  public FastFeatureVectors getFfVecs() {
    return this.ffVecs;
  }

  /**
   * Our Solution (try to add more dependency into edges)
   * find more dependencies because of <instanceof>
   */
  private void runOurSolution() throws IOException {
    String projectPath = "../tomcat/src/8.5.47/"; // subject to change
    Tomdog dog = new Tomdog(projectPath); // our core processor

    // find more dependencies
    System.out.println("\n--------- Our Solution Starts ---------");
    System.out.println("\nWe Found: " + dog.getInterfaceSet().size() + " interfaces & " + dog.getInstanceofMap().keySet().size() + " instanceofs");
    System.out.println("\nTrying to find more dependencies in them...");
    List<List<String>> moreDependencies = dog.findMoreDependencies();
    System.out.println("\nWe Found: " + moreDependencies.size() + " new dependencies because of <instanceof>!");

    // add them to edges
    for (List<String> depend : moreDependencies) {
      Pair<String, String> edge = new ImmutablePair<>(depend.get(0), depend.get(1));
      edges.add(edge);
    }

    System.out.println("\n--------- End of Our Solution ---------\n");

  }

}
