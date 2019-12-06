/**
 * @author Junhao Wang
 * @date 12/05/2019
 */
package OurSolution;

import acdc.Node;
import acdc.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.util.*;


/**
 * This pattern helps discover "instanceof" patterns in dependency.
 */
public class InstanceofPattern extends Pattern {

  public InstanceofPattern(DefaultMutableTreeNode _root) {
    super(_root);
    name = "Instanceof Pattern";
  }

  public void execute() {
    /** read all dependencies */
    // there are two ways
    // 1. Read through all dependencies in 'dep.rsf' file
    //    and find dependencies that satisfy this pattern
    // 2. Since Tomdog has already analyzed this, we can
    //    get all the new dependencies from it.

    Tomdog dog = null;
    try {
      dog = Tomdog.getInstanceDog();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (dog == null) return;

    List<List<String>> newDependencies = dog.findMoreDependencies();

    // convert it to a hash map for faster performance
    Map<String, Set<String>> dependMap = new HashMap<>();
    for (List<String> dep : newDependencies) {
      Set<String> set = dependMap.getOrDefault(dep.get(0), new HashSet<>());
      set.add(dep.get(1));
      dependMap.put(dep.get(0), set);
    }


    // mapping class name to node
    Map<String, Node> nodeMap = new HashMap<>();
    Vector allNodes = Pattern.allNodes(root);
    for (int i = 0; i < allNodes.size(); ++i) {
      Node n = (Node) allNodes.get(i);
      if (n.isCluster() == false) continue;
      nodeMap.put(n.getName(), n);
    }


    // tree
    Vector rootChildren = nodeChildren(root);

    for (int i = 0; i < rootChildren.size(); ++i) {
      Node nparent = (Node) rootChildren.elementAt(i);
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) nparent.getTreeNode();

      if (nparent.isCluster() == false) continue;

      Vector subChildren = nodeChildren(parent);

      Set<String> toBeAdded = new HashSet<>();

      System.out.println("parent: " + nparent.getName());

      for (int j = 0; j < subChildren.size(); ++j) {
        Node ncurr = (Node) subChildren.elementAt(j);
        DefaultMutableTreeNode curr = (DefaultMutableTreeNode) ncurr.getTreeNode();

        // System.out.println("--------> " + ncurr.getName());
        if (dependMap.containsKey(ncurr.getName())) {
          toBeAdded.addAll(dependMap.get(ncurr.getName()));
        }
      }

      // System.out.println(">>>> " + toBeAdded);

      // add to it
      System.out.println("hi");
      for (String clsName : toBeAdded) {
        // Node node = new Node(clsName, "Unknown");
        Node node = nodeMap.get(clsName);
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        treeNode.setParent(parent);
        treeNode.setAllowsChildren(true);

        // parent.add(treeNode);
      }
    }




    System.out.println("><>>><<><><<<<<<<<<<<<<<<<<<<<");

  }



}





