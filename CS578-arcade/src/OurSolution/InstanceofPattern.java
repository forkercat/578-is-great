/**
 * @author Junhao Wang
 * @date 12/05/2019
 */
package OurSolution;

import acdc.IO;
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
    //    get all the new dependencies from it. (x)  <-- we choose this one

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

    IO.put("\n --> There are " + newDependencies.size() + " new dependencies to be added.\n   (set IO output level as 2 for information)\n", 0);


    // go through the tree and
    // if there is a dependency A -> B
    //    && parent P has a child A,
    // then we add B to P.children list
    Vector rootChildren = nodeChildren(root);

    for (int i = 0; i < rootChildren.size(); ++i) {
      Node nparent = (Node) rootChildren.elementAt(i);
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) nparent.getTreeNode();

      if (nparent.isCluster() == false) continue; // not a cluster

      Vector subChildren = nodeChildren(parent);

      Set<String> toBeAdded = new HashSet<>();

      for (int j = 0; j < subChildren.size(); ++j) {
        Node ncurr = (Node) subChildren.elementAt(j);
        DefaultMutableTreeNode curr = (DefaultMutableTreeNode) ncurr.getTreeNode();

        if (dependMap.containsKey(ncurr.getName())) {
          toBeAdded.addAll(dependMap.get(ncurr.getName()));
        }
      }

      // add to it
      for (String clsName : toBeAdded) {
        Node node = new Node(clsName, "Unknown");
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        treeNode.setAllowsChildren(false);
        node.setTreeNode(treeNode);
        parent.add(treeNode);
        treeNode.setParent(parent);
      }
    }

    System.out.println("(This is our solution!)");
  }



}





