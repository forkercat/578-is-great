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
  - [Major Task](#major-task)
    - [Security Decision #1 (System Property Disclosure)](#security-decision-1-system-property-disclosure)
      - [Function Description](#function-description)
      - [Vulnerability Description](#vulnerability-description)
      - [How It Was Fixed](#how-it-was-fixed)
      - [Failure in Recovery Techniques](#failure-in-recovery-techniques)
      - [What We Did](#what-we-did)
        - [Recognize A New Pattern](#recognize-a-new-pattern)
        - [Walkthrough in ACDC Code](#walkthrough-in-acdc-code)
        - [Solutions That We Tried](#solutions-that-we-tried)
      - [What We Didn’t](#what-we-didnt)
    - [Security Decision #2 (JREPlatform.java)](#security-decision-2-jreplatformjava)
      - [Function Description](#function-description-1)
      - [Vulnerability Description](#vulnerability-description-1)
      - [How It Was Fixed](#how-it-was-fixed-1)
  - [Extra Work](#extra-work)
    - [Visualization](#visualization)
  - [Interesting Stuff](#interesting-stuff)

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
  - Download Tomcat [source files]() (it is compiled, no worry), unzip, and put the folder `src` in `578-is-great/tomcat`.
  - Download [library files](), unzip, and put them in `578-is-great/CS578-arcade`.
- Open `578-is-great/CS578-arcade` with IntelliJ IDEA (`.idea` exists, do not re-create).
- Enjoy:)

Other Tools We Used:

- [draw.io](https://www.draw.io/)



## Major Task

Link: [Tomcat 8.x Vulnerabilities](http://tomcat.apache.org/security-8.html)

### Security Decision #1 (System Property Disclosure)

Vulnerability ID: [CVE-2016-6794](http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2016-6794)

This was fixed in revision [1754726](https://svn.apache.org/viewvc?view=revision&revision=1754726) for 8.5.x. Open it to see which files are related.

#### Function Description

<!-- 设计到哪些类和大概的功能 -->
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


#### Vulnerability Description

<!-- 安全缺陷的描述（要自己写, 加引用） -->
There is a method `getProperty()` in `Digester` class. In old versions, this method will simply call `System.getProperty()` and return the result.

It is possible that a malicious application could get a `Digester` object and call `getProperty()` through the newly created `Digester` object. Then it gets system properties that should be invisible.

#### How It Was Fixed

<!-- PermissionCheck的原理 -->
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


#### Failure in Recovery Techniques

In the original ACDC technique, it successfully clusters `Digester` and `PermissionCheck`, but it fails in putting `WebappClassLoaderBase` into that cluster.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/or8s5.png)

**Note:** Visualization is in `Extra Work` section.

Therefore, our goal is to put these three components together in a cluster.



#### What We Did

##### Recognize A New Pattern

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

##### Walkthrough in ACDC Code

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
    - Iterate all selected patterns and build a tree
    - Generate clusters from the tree starting from `root`
    - Output into files
    - Smell Detection

##### Solutions That We Tried




#### What We Didn’t







### Security Decision #2 (JREPlatform.java)

#### Function Description

<!-- 设计到哪些类和大概的功能 -->
This security decision is related to classes including `AbstractFileResourceSet` and `JrePlatform`.

The `AbstractFileResourceSet` is used to get the file identified by a URL.

The `JrePlatform` is used to check whether the tomcat is running on windows.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/xj9uh.png)

#### Vulnerability Description

<!-- 安全缺陷的描述（要自己写, 加引用） -->
It's possible that a user upload a JSP file through HTTP PUT and that JSP file could be executed later.

In this case, if the URL of that JSP file does not meet the requirement of the server's platform environment, like windows, an error could rise and the tomcat might crash.


#### How It Was Fixed

<!-- JrePlatform的原理 -->
Tomcat added another class named `JrePlatform` to solve this problem.

Another component is introduced when fetching a file through URL. The `AbstractFileResourceSet` will fetch the IS_WINDOWS variable, which would be initialized through the static method in `JrePlatform` when this class is being loaded.

Then the `AbstractFileResourceSet` will check if the current platform is windows. If so, the `AbstractFileResourceSet` will use windows directory naming rule to check the URL passed in. If the validation does pass, `AbstractFileResourceSet` will simply return null. If the validation passes, the AbstractResourceSet will fetch the real file.

```java
if (JrePlatform.IS_WINDOWS && isInvalidWindowsFilename(name)) {
	return null;
}
```

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/cgf50.png)












## Extra Work

### Visualization



## Interesting Stuff

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/gye11.png)

