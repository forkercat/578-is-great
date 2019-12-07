# 578-is-great


This is a course project based on `ARCADE` and `Tomcat` in CS 578 Software Architecture.

**Instructor & TA:** [Nenad Medvidovic](mailto:neno@usc.edu), [Adriana Sejfia](mailto:sejfia@usc.edu)

**Authors:** `Junhao Wang`, `Han Hu`, `Hopong Ng` (names not listed in order)

**Contact Us:** [junhaowanggg@gmail.com](mailto:junhaowanggg@gmail.com)

**Reference:** listed in each section if needed.

**Table of Contents:**

- [578-is-great](#578-is-great)
  - [Project Description](#project-description)
  - [Summary of What We Did](#summary-of-what-we-did)
  - [Before We Start](#before-we-start)
  - [Security Decision #1](#security-decision-1)
    - [Function Description](#function-description)
    - [Vulnerability Description](#vulnerability-description)
    - [How It Was Fixed](#how-it-was-fixed)
    - [Failure in Recovery Techniques](#failure-in-recovery-techniques)
    - [What We Did](#what-we-did)
      - [Recognize A New Pattern](#recognize-a-new-pattern)
      - [Walkthrough in ACDC Code](#walkthrough-in-acdc-code)
      - [Solutions That We Tried](#solutions-that-we-tried)
    - [Potential Improvement](#potential-improvement)
  - [Security Decision #2](#security-decision-2)
    - [Function Description](#function-description-1)
    - [Vulnerability Description](#vulnerability-description-1)
    - [How It Was Fixed](#how-it-was-fixed-1)
    - [Failure in Recovery Techniques](#failure-in-recovery-techniques-1)
    - [What We Did](#what-we-did-1)
    - [Potential Improvement](#potential-improvement-1)
  - [Extra Work: Visualization](#extra-work-visualization)
    - [Cluster Bubbles](#cluster-bubbles)
    - [Dependencies on Class Packages](#dependencies-on-class-packages)
  - [Interesting Stuff](#interesting-stuff)

**Note:** Search `Where We Modified` in this page to see the code we have modified.

## Project Description

In our third homework, we applied two recovery techniques to Tomcat and recovered Tomcat's architectures in the form of clusters. However, these techniques are not very suitable to analyze the security of a system from an architecture perspective (e.g. some techniques cannot cluster components of an architectural decision together).

In this project, we modify the existing techniques in `ARCADE` and turn them into "security-aware" versions. Specifically, given a security-related architectural decision, our refined techniques can put all its components into a cluster.

**Our Subject System:** Apache Tomcat 8.5.47

## Summary of What We Did

- Read source code of Tomcat, and understand how Tomcat works.
- Pick security-related vulnerabilities, analyze if they are suitable for this project.
- Know how relevant components work and why the vulnerabilities occur and how they affect the system.
- Show that original techniques fail in discovering the vulnerabilities.
- Analyze source code in `ARCADE` and understand how `ACDC` and `ARC` work.
- Develop solutions that improve the techniques, write many comments, and make output more readable.🙄
- Use [D3.js](https://d3js.org/) to create visualizations (super cool).


## Before We Start

Setting up [ARCADE](https://github.com/asejfia/CS578-arcade) in our working environment is extremely hard. Since we would re-compile source files for many times, we open the project files in IntelliJ IDEA. And yes... it took us few days to make it work.

So if you want to run our code, you can follow these steps:

- Clone our this repository to your local directory by `git clone git@github.com:junhaowww/578-is-great.git` .
- Since the project is so large, we separate Tomcat and some library files from our repository.
  - Download Tomcat source files [tomcat_src.zip](https://drive.google.com/open?id=1G8iim7zwuyFeVv-iV1O9XDVJ95lbTgUG) (it is compiled, no worry), unzip, and put the folder `src` in `578-is-great/tomcat`.
  - Download library files [library.zip](https://drive.google.com/open?id=1gYTkVeeNyrBW4_h2lD78NXvPzTlwhacl), unzip, and put them in `578-is-great/CS578-arcade`.
- Open `578-is-great/CS578-arcade` with IntelliJ IDEA (`.idea` exists, do not re-create).
- Enjoy:)

Other Tools We Used:

- [draw.io](https://www.draw.io/)
- [Observable](https://observablehq.com/)
- [XML Viewer](https://codebeautify.org/xmlviewer)


Link: [Tomcat 8.x Vulnerabilities](http://tomcat.apache.org/security-8.html)





## Security Decision #1

**Name:** System Property Disclosure

**Vulnerability ID:** [CVE-2016-6794](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2016-6794)

This was fixed in revision [1754726](https://svn.apache.org/viewvc?view=revision&revision=1754726) for 8.5.x. Open it to see which files are related.

### Function Description

This security decision is related to several classes including `WebappClassLoaderBase`, `PermissionCheck`, `Digester`.

`WebappClassLoader` is a shared class loader. When Tomcat loads an app, `WebappClassLoaderBase` will create `Digester` to parse XML files and use `Digester` to create and set up other components like Server, Connector, Container, etc.

`PermissionCheck` is an interface implemented by `WebappClassLoaderBase`. It will be used for permission check when a `Digester` object calls the method `getProperty()`.

In previous versions, there is no `PermissionCheck`, so the code is as follows:

```java
// Digester.java
@Override
public String getProperty(String key) {
  return System.getProperty(key);
}
```

Class Diagram:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/sv36i.png)


### Vulnerability Description

There is a method `getProperty()` in `Digester` class. In old versions, this method will simply call `System.getProperty()` and return the result.

It is possible that a malicious application could get a `Digester` object and call `getProperty()` through the newly created `Digester` object. Then it gets system properties that should be invisible.

### How It Was Fixed

Tomcat added an interface `PermissionCheck`, and `WebappClassLoaderBase` implements this interface and overwrites `check()` method stated in the interface. Also, Tomcat modifies the method `getProperty()` in `Digester` class. The code is as follows:

```java
@Override
public String getProperty( String key ) {
  ClassLoader cl = Thread.currentThread().getContextClassLoader();
  if (cl instanceof PermissionCheck) {
    Permission p = new PropertyPermission(key, "read");
    if (!((PermissionCheck) cl).check(p)) {
      return null;
    }
  }
  return System.getProperty(key);
}
```

When this method is called, it will firstly find the class loader through `Thread.currentThread().getContextClassLoader()`. Then it will check if this class loader object implements `PermissionCheck`. If so, this loader is actually a `WebappClassLoaderBase` object and `Digester` will call `check()` to see if the application is granted permission to access system properties.

Class Diagram:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/it0or.png)

**Note:** `WebAppClassLoaderBase` should be `WebappClassLoaderBase`.


### Failure in Recovery Techniques

In the original ACDC technique, it successfully clusters `Digester` and `PermissionCheck`, but it fails in putting `WebappClassLoaderBase` into that cluster.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/or8s5.png)

**Note:** Visualization is in `Extra Work` section.

Therefore, our goal is to put these three components together in a cluster.



### What We Did

#### Recognize A New Pattern

It took us a long period of time to think about why this technique failed. We discovered a pattern that the technique could not recognize.

Code:

```java
@Override
public String getProperty( String key ) {
  ClassLoader cl = Thread.currentThread().getContextClassLoader();
  if (cl instanceof PermissionCheck) {
    Permission p = new PropertyPermission(key, "read");
    if (!((PermissionCheck) cl).check(p)) {
      return null;
    }
  }
  return System.getProperty(key);
}
```

The interesting thing is that by static analysis we cannot know that `Digester` actually has some connection with `WebappClassLoaderBase`. It turns out that the object `cl` is an object that implements the interface `PermissionCheck`, so we would say `Digester` implicitly "works with" `WebappClassLoaderBase` and should depend on it.

Diagram:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/jt3sh.png)

This pattern occurs when the following constraints are satisfied:

- `A` depends on an interface `C` (maybe by `import` or something else)
- `B` implements the interface `C` ("implements" is also a kind of dependency)
- `A` has some code like `? instanceof X` and `X` is the interface `C`

So we came up with some solutions that we thought might work. But before that we wanted to go over the code in `ACDC`.

#### Walkthrough in ACDC Code

- **AcdcWithSmellDetection.java**
  - `main()`
    - Read files
    - `single()`
      - `builder.build(builderArgs)`
        - Build dependencies by a third-party library `Classycle`
        - Store dependencies in an output file `[version]_deps.rsf`
      - Run ACDC by `ACDC.main(acdcArgs)`
- **ACDC.java**
  - `main()`
    - Set up parameters
    - Pick patterns we want to use (`BodyHeader`, `SubGraph`, `OrphanAdoption`)
      - `BodyHeader` is useless since it is based on `.h` and `.c` files.
    - Iterate all selected patterns and build a tree
    - Generate clusters from the tree starting from `root`
    - Output into files
    - Smell Detection


#### Solutions That We Tried

- **Solution #1:** Add dependencies before running ACDC. For example, we add dependency `Digester -> WebappClassLoaderBase`. However, it won't work. ACDC still cannot find this pattern.

- **Solution #2:** Modify existing patterns in ACDC. It turns out that these patterns are extremely difficult to be understood. Although we have tried, we still have no ideas how they work in essential.

- **Solution #3:** We develop a tool that discovers the pattern (we may call it "instanceof" pattern), and find a way of adding new dependencies into the tree. _It works!_

**Where We Modified:** Our modification in ACDC mainly lies in [AcdcWithSmellDetection.java](https://github.com/junhaowww/578-is-great/blob/master/CS578-arcade/src/edu/usc/softarch/arcade/AcdcWithSmellDetection.java) and [ACDC.java](https://github.com/junhaowww/578-is-great/blob/master/CS578-arcade/src/acdc/ACDC.java). Code are commented, so it should be readable. In addition, in folder [OurSolution](https://github.com/junhaowww/578-is-great/tree/master/CS578-arcade/src/OurSolution) we create our own classes.

Basically, we have a singleton class `Tomdog` ([Tomdog.java](https://github.com/junhaowww/578-is-great/blob/master/CS578-arcade/src/OurSolution/Tomdog.java)) that discovers the "instanceof" pattern. It is designed as a singleton because it then can be reused readily.

To find this pattern, `Tomdog` analyzes `.java` source files and uses regular expression to collect all interfaces and find patterns like `A implements B, C, D` and `A instanceof B`. All this information is stored in several hash maps and hash sets for future usage.

```java
// Example code snippet for finding "instanceof" in source code
private void processWithInstanceof(String codeStr, String className) {
  // package name
  String packageName = getPackageName(codeStr);
  String fullClassName = packageName + "." + className;
  Map<String, String> nameMap = classNameMap.get(fullClassName);

  // instanceof
  Pattern instanceofPattern = Pattern.compile("[.\\s]* (\\((.*)\\s+instanceof\\s+(.*)\\)) [.\\s]*");
  Matcher m2 = instanceofPattern.matcher(codeStr);

  while (m2.find()) {
    String interfaceName = nameMap.get(m2.group(3).split("<")[0]);
    // check if it is an interface
    if (interfaceName != null && interfaceSet.contains(interfaceName)) {
      Set<String> set = instanceofMap.getOrDefault(fullClassName, new HashSet<>());
      set.add(interfaceName);
      instanceofMap.put(fullClassName, set);
    }
  }
}
```

Also, `Tomdog` finds new dependencies based on the pattern we discussed above. For example, it adds a dependency `Digester -> WebappClassLoaderBase`.

Then `Tomdog` is sad because we will put it aside for a while. In `ACDC.java`, we need to put the new dependencies into the tree.

```java
// Example code snippet for adding patterns
Vector vpatterns = new Vector();
for (int j = 0; j < selectedPatterns.length(); j++) {
  switch (selectedPatterns.charAt(j)) {
    case 'h':
      vpatterns.add(new BodyHeader(root)); break;
    case 's':
      vpatterns.add(new SubGraph(root, maxClusterSize)); break;
    case 'o':
      vpatterns.add(new OrphanAdoption(root)); break;
    case 'b': // our pattern
      vpatterns.add(new InstanceofPattern(root)); break;
    default:
      IO.put("Serious error.", 0);
      System.exit(0);
  }
}
```

You may notice that we have a new pattern called `InstanceofPattern` ([InstanceofPattern.java](https://github.com/junhaowww/578-is-great/blob/master/CS578-arcade/src/OurSolution/InstanceofPattern.java)). `InstanceofPattern` is a class that extends an abstract class `Pattern`. It should implement a method called `execute()`.

```java
public class InstanceofPattern extends Pattern {
  public void execute() { // ... }
}
```

Its task is to add the dependency to the tree, but it is quite challenging. In addition, at this time `Tomdog` awakes and we use `findMoreDependencies()` to fetch the new dependencies we found before.

```java
// Example code snippet for finding new dependencies
// Returns a list that contains [A, B], [C, D], ...
//         where A depends on B, C depends on D
public List<List<String>> findMoreDependencies() {
  if (newDependencies != null) {
    return newDependencies;
  }
  newDependencies = new ArrayList<>();
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
          newDependencies.add(Arrays.asList(classHasInstanceof, cls));
        }
      }
    }
  }
  return newDependencies;
}
```

**Finally,** we re-run ARCADE and have the result we would expect as follows.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/uxriq.png)

Here is the output in console:

```shell
--------- [start] Tomdog is barking!!! ---------

wait...

We Found: 181 interfaces & 68 "instanceofs" relationship patterns

--------- [end] Tomdog now starts sleeping :) ---------

// ...

--------- [start] Our Solution (add more dependencies) ---------

We Found: 353 new dependencies because of <instanceof>!

--------- [end] Our Solution ---------

Finish building dependencies... :)

Starting ACDC...
ardcArgs: [/Users/chuck/Desktop/578-is-great/tomcat/output/ACDC/8.5.47_deps.rsf, /Users/chuck/Desktop/578-is-great/tomcat/output/ACDC/8.5.47_acdc_clustered.rsf]

Running ACDC for revision 8.5.47(please wait T_T )

Include all edges...

 --> The following 3571 nodes were selected for edge induction: 
   (set IO output level as 2 for information)


--------- [start] Using Patterns ---------


[1] Executing:  [Subgraph Dominator] pattern...

 --> The following 2050 nodes were selected for edge induction: 
   (set IO output level as 2 for information)


[2] Executing:  [Orphan Adoption] pattern...

 --> The following 243 nodes were selected for edge induction: 
   (set IO output level as 2 for information)


[3] Executing:  [Instanceof Pattern] pattern...

 --> There are 353 new dependencies to be added.
   (set IO output level as 2 for information)

(This is our solution!)

--------- [end] Using Patterns ---------

// ...
```

### Potential Improvement

We think this new pattern we found is great. However, our technique should be improved. Consider the following case:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/9sdxe.png)

If there exists other classes `D` and `E` that implement `C` and `B`, `D`, `E` are very different. It is obvious that `A` does not depend on `D` and `E`, but in our current method these meaningless dependencies are created as well.

To improve, we can add a constraint to check `X`'s type in the pattern `X instanceof Y`; however, figuring out the type of `X` just by searching is not easy.

Last but not least, our way of adding the new dependencies to the tree should be improved either. The current approach is quite tricky and counter-intuitive. Maybe we can directly modify the exiting patterns like `SubGraph`.









## Security Decision #2

**Name:** Remote Code Execution

**Vulnerability ID:** [CVE-2017-12617](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-12617)

This was fixed in revision [1809921](https://svn.apache.org/viewvc?view=revision&revision=1809921) for 8.5.x. Open it to see which files are related.


### Function Description

This security decision is related to two classes `AbstractFileResourceSet` and `JrePlatform`.

- `AbstractFileResourceSet` is used to get the file identified by a URL.

- `JrePlatform` is used to check whether the Tomcat system is running on Windows operating systems.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/xj9uh.png)



### Vulnerability Description

It is possible that a user upload a JSP file through an HTTP PUT request and that the JSP file could be executed in the system later.

In this case, if the URL of that JSP file does not meet the requirement of the server's platform environment (e.g. Windows), an error would arise and the Tomcat might crash.


### How It Was Fixed

Tomcat added another class named `JrePlatform` to solve this problem.

Another component is introduced when fetching a file through URL. The `AbstractFileResourceSet` will fetch the IS_WINDOWS variable, which would be initialized through the static method in `JrePlatform` when this class is being loaded.

Then the `AbstractFileResourceSet` will check if the current platform is windows. If so, the `AbstractFileResourceSet` will use windows directory naming rule to check the URL passed in. If the validation does pass, `AbstractFileResourceSet` will simply return null. If the validation passes, the AbstractResourceSet will fetch the real file.

```java
if (JrePlatform.IS_WINDOWS && isInvalidWindowsFilename(name)) {
  return null;
}
```

Class Diagram:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/cgf50.png)



### Failure in Recovery Techniques
In the original ACDC technique, ACDC fails in clustering `AbstractFileResourceSet` and `JrePlatform`.


### Problem Analysis
The reason why `AbstractFileResourceSet` and `JrePlatform` are not clustered is that in SubGraph Pattern, every child of the root will find its coverSet. A coverSet is a HashSet containing a node called "dominator node" and the set of its dominated nodes, `N = n(i), i:1,2,...m`, which have the followling properties.
1. There exists a path from the doninator to every n(i)
2. For any node v such that there exists a path from v to any n(i), either dominator node is in that path or v is one of n(i).
In our testcase, the coverSet `org.apache.tomcat.util.compat.JrePlatform` finds is 
`(org.apache.tomcat.util.compat.JrePlatform, org.apache.tomcat.util.compat.JrePlatform$1)`. 
After finding its coverSet, a cluster named `org.apache.tomcat.util.compat.ss` will be created which contains `org.apache.tomcat.util.compat.JrePlatform` and `org.apache.tomcat.util.compat.JrePlatform$1`.
As a result, `org.apache.tomcat.util.compat.JrePlatform` will not appear in any other cluster. So `AbstractFileResourceSet` and `JrePlatform` are not in the same cluster in output.


### What We Did
We modify the `SubGraph Pattern` so that it will not create a cluster if the coverSet only contains twe elements, `A` and `A$1`. After our modification, the cluster named `org.apache.tomcat.util.compat.ss` will not be created in the `SubGraph Pattern`.
Both `org.apache.tomcat.util.compat.JrePlatform` and `org.apache.tomcat.util.compat.JrePlatform$1)` will become `orphans` which will be clustered in the `OrphanAdoption Pattern` following the rule that an orphan would be placed under the cluster-node with the largest number of children which point to the orphan.
Here is our modification in the `SubGraph Pattern`.
```java
HashSet cS = coveredSet(tentativeDominatorTreeNode, vRootChildren);

DefaultMutableTreeNode aa;
Node cSNode;
Iterator al = cS.iterator();
while (al.hasNext()) {
	aa = (DefaultMutableTreeNode) al.next();
   	cSNode = (Node) aa.getUserObject();
    	int index = cSNode.getName().lastIndexOf('$');
    	if(cSNode.getName().contains(tentativeDominator.getName()+'$')&&cS.size() == 2){
     		cS.remove(aa);
     		break;
    	}
}
```
Finally we re-run ARCADE and have the result we would expect as follows.


## Extra Work: Visualization

With the help of [D3.js](https://d3js.org/) and [Observable](https://observablehq.com/), we built some useful illustrations.

To generate data for visualization, we create a `GetJSON` ([GetJSON.java](https://github.com/junhaowww/578-is-great/blob/master/CS578-arcade/src/OurSolution/GetJSON.java)) class that converts our output information to JSON.

### Cluster Bubbles

Play With It: [[CS578] Cluster Bubbles](https://observablehq.com/@junhaowww/cs578-cluster-bubbles)

- It helps you pick up information of all clusters readily.
- It is based on Word Count Bubble Chart & Generated Tags Cluster for NLP Projects of NASA. It is a fork of [Word Count Bubble Chart by wolfiex](https://observablehq.com/@wolfiex/word-count-bubble-chart) that adds mouseover behavior.

Originally, we have to find clusters from an output file like this:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/af28b.png)

Visualization:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/w8ce1.png)


### Dependencies on Class Packages

Play With It: [[CS578] Dependencies on Class Packages](https://observablehq.com/@junhaowww/cs578-dependencies-on-class-packages)

- It helps you figure out dependencies between packages.
- It is based on MouseOver Chord Diagram. It is a fork of [chord diagram](/@mbostock/d3-chord-diagram) that adds mouseover behavior.

Originally, we have to find dependencies from an output file like this:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/mi5hp.png)

Visualization:

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/wn9th.png)


## Interesting Stuff

Each of us worked on each part and then we taught others what we learned.

Very struggling...

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/gye11.png)

