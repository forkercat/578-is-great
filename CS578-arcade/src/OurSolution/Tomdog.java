package OurSolution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Junhao Wang
 * @date 12/04/2019
 */

public class Tomdog {

  public static void main(String[] args) throws IOException {
    // String projectPath = "/Users/chuck/Desktop/Code/w1/src/files/";
    String projectPath = "/Users/chuck/Desktop/578-is-great/tomcat/src/8.5.47/";
    Tomdog dog = new Tomdog(projectPath);

    // System.out.println("\nInterfaces:");
    // System.out.println(dog.getInterfaceSet());
    // System.out.println("\nImplements:");
    // System.out.println(dog.getImplMapping());
    // System.out.println("\nInstanceof:");
    // System.out.println(dog.getInstanceofMap());

    System.out.println("\nDone...");
    System.out.println("\nWe found in .java source files: " + dog.getImplMapping().keySet().size() + " implements | " + dog.getInterfaceSet().size() + " interfaces | " + dog.getInstanceofMap().keySet().size() + " instanceofs");
  }

  // <classFullName, map<usedClassName, usedClassFullName>>
  private Map<String, Map<String, String>> classNameMap = new HashMap<>();
  // <interface, [implementing classes]>
  private Map<String, Set<String>> implementMap = new HashMap<>();
  // <interface>
  private Set<String> interfaceSet = new HashSet<>();
  // <current class, [interface]>
  private Map<String, Set<String>> instanceofMap = new HashMap<>();


  public Tomdog(String path) throws IOException {
    // get all files
    List<File> fileList = new ArrayList<>();
    getAllJavaFiles(path, fileList);

    // build class usage
    for (File file : fileList) {
      String className = classNameFromFile(file);
      String codeStr = readFileToString(file.toString());
      generateClassNameUsage(codeStr, className);
    }

    // build interfaces & implements map
    for (File file : fileList) {
      String className = classNameFromFile(file);
      String codeStr = readFileToString(file.toString());
      processWithInterface(codeStr, className);
    }

    for (File file : fileList) {
      String className = classNameFromFile(file);
      String codeStr = readFileToString(file.toString());
      processWithInstanceof(codeStr, className);
    }
  }

  public List<List<String>> findMoreDependencies() {
    List<List<String>> result = new ArrayList<>();

    // for each instanceof
    for (String classHasInstanceof : instanceofMap.keySet()) {
      Set<String> interfaces = instanceofMap.get(classHasInstanceof);
      // for each interface
      for (String inf : interfaces) {
        if (implementMap.containsKey(inf)) {
          // if yes, there is a bunch of classes implementing this interface
          Set<String> classThatImplInterface = implementMap.get(inf);
          for (String cls : classThatImplInterface) {
            // classHasInstanceof depends on cls
            result.add(Arrays.asList(classHasInstanceof, cls));
          }
        }
      }
    }
    return result;
  }


  private void generateClassNameUsage(String codeStr, String className) {
    // package name
    String packageName = getPackageName(codeStr);
    String fullClassName = packageName + "." + className;

    // import string
    Map<String, String> map = classNameMap.getOrDefault(fullClassName, new HashMap<>());
    Pattern importPattern = Pattern.compile("[.\\s]import (.*);[.\\s]");
    Matcher m2 = importPattern.matcher(codeStr);
    while (m2.find()) {
      String importStr = m2.group(1);
      String name = importStr.substring(importStr.lastIndexOf('.') + 1, importStr.length());
      map.put(name, importStr);
    }

    classNameMap.put(fullClassName, map);
  }


  private void processWithInterface(String codeStr, String className) {
    // package name
    String packageName = getPackageName(codeStr);
    String fullClassName = packageName + "." + className;
    Map<String, String> nameMap = classNameMap.get(fullClassName);

    // interface
    Pattern interfacePattern = Pattern.compile("[.\\s]* (class (.*)\\s+implements\\s+(.*)) [.\\s]*");
    Matcher m2 = interfacePattern.matcher(codeStr);

    while (m2.find()) { // pattern: left implements right
      // left
      String[] left = m2.group(2).split("\\s+");
      left = left[0].split("<");
      if (left[0].equals(className) == false) continue; // we only consider the public class of the file
      // right
      String[] right = m2.group(3).split(",\\s+");
      // put in
      for (String s : right) {
        // interface set
        String interfaceName = nameMap.get(s.split("<")[0]); // convert to full name
        if (interfaceName != null) {
          interfaceSet.add(interfaceName);
          // implementing
          Set<String> implementingSet = implementMap.getOrDefault(interfaceName, new HashSet<>());
          implementingSet.add(fullClassName);
          implementMap.put(interfaceName, implementingSet);
        }
      }
    }
  }


  private void processWithInstanceof(String codeStr, String className) {
    // package name
    String packageName = getPackageName(codeStr);
    String fullClassName = packageName + "." + className;
    Map<String, String> nameMap = classNameMap.get(fullClassName);

    // instanceof
    Pattern instanceofPattern = Pattern.compile("[.\\s]* (\\((.*)\\s+instanceof\\s+(.*)\\)) [.\\s]*");
    Matcher m2 = instanceofPattern.matcher(codeStr);

    while (m2.find()) {
      // String leftClass = m2.group(2);
      String interfaceName = nameMap.get(m2.group(3).split("<")[0]);
      // check if it is an interface
      if (interfaceName != null && interfaceSet.contains(interfaceName)) {
        Set<String> set = instanceofMap.getOrDefault(fullClassName, new HashSet<>());
        set.add(interfaceName);
        instanceofMap.put(fullClassName, set);
      }
    }
  }

  private List<File> getAllJavaFiles(String fileDir, List<File> fileList) {
    File file = new File(fileDir);
    File[] files = file.listFiles();
    if (files == null) {
      return null;
    }
    for (File f : files) {
      if (f.isFile() && f.toString().endsWith(".java")) {
        fileList.add(f);
      } else if (f.isDirectory()) {
        getAllJavaFiles(f.getAbsolutePath(), fileList);
      }
    }
    return fileList;
  }

  private static String getPackageName(String codeStr) {
    Pattern packagePattern = Pattern.compile("[.\\s]package (.*);[.\\s]");
    Matcher m1 = packagePattern.matcher(codeStr);
    if (m1.find() == false) return null;
    return m1.group(1);
  }

  private static String classNameFromFile(File file) {
    String[] arr = file.toString().split("/"); // class name
    String fileName = arr[arr.length - 1];
    String className = fileName.split("\\.")[0];
    return className;
  }

  private static String readFileToString(String file) throws IOException {
    String contents = new String(Files.readAllBytes(Paths.get(file)));
    return contents;
  }


  public Map<String, Set<String>> getImplMapping() {
    return implementMap;
  }

  public Set<String> getInterfaceSet() {
    return interfaceSet;
  }

  public Map<String, Set<String>> getInstanceofMap() {
    return instanceofMap;
  }
}




