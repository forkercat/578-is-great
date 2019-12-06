# 578-is-great

This is a course project based on `ARCADE` and `Tomcat` in CS 578.

**Instructor & TA:** [Nenad Medvidovic](mailto:neno@usc.edu), [Adriana Sejfia](mailto:sejfia@usc.edu)

**Authors:** Junhao Wang, Han Hu, Haobang Wu (names not listed in order)

**Contact Us:** [junhaowanggg@gmail.com](mailto:junhaowanggg@gmail.com)

**Reference:** listed in each section.




## Summary

项目介绍
大概我们做了什么

## Before We Start

环境配置和安装
用了什么工具


## Major Task

### Security Decision #1 (PermissionCheck.java)

#### Function Description

<!-- 设计到哪些类和大概的功能 -->
This security decision is related to serveral classes including WebAppClassLoaderBase, PermissionCheck, Digester.

The WebAppClassLoader is a shared classloader. When tomcat wants to load an app, the WebAppClassLoader will create a Digester for xml parsing and use the Digester to create other components like Server, Connector, Container, etc.

PermissionCheck is an interface implemented by WebClassLoaderBase. It will be used for permission check when a digester calls its getProperty() method.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/sv36i.png)


#### Vulnerability Description

<!-- 安全缺陷的描述（要自己写, 加引用） -->
There is a method getProperty() in Digester class. In old versions, this method will simply call System.getProperty() and return the result.

It is possible that a malicious app could get a Digester object. Then this app could call the getProperty() through the newly created digester object and get system properties that should be invisible to him.

#### How It Was Fixed

<!-- PermissionCheck的原理 -->
Tomcat added an interface named PermissionCheck and the WebAppClassLoaderBase implemented the check() method stated in PermissionCheck interface.

Then tomcat modified the getProperty() method in Digester class. 

When this method is called, it will firstly find the class loader of this app through Thread.currentThread().getContextClassLoader(). 

Then it'll check if this class loader is a WebAppClassLoaderBase. If so, the digester will call the check() method implemented by the classloader to check if he is granted to visite system properties. If the digester cannot pass the permission check, this method will simply return null. If the digester passes the permission check, this method will call System.getProperty() and return the its result.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/it0or.png)


#### Failure of Recovery



#### What We Did



#### What We Didn’t







### Security Decision #2 (JREPlatform)

#### Function Description

<!-- 设计到哪些类和大概的功能 -->
This security decision is related to classes including AbstractFileResourceSet and JrePlatform.

The AbstractFileResourceSet is used to get the file identified by a URL.

The JrePlatform is used to check whether the tomcat is running on windows.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/xj9uh.png)

#### Vulnerability Description

<!-- 安全缺陷的描述（要自己写, 加引用） -->
It's possible that a user upload a JSP file through HTTP PUT and that JSP file could be executed later.

In this case, if the URL of that JSP file does not meet the requirement of the server's platform environment, like windows, an error could rise and the tomcat might crash.


#### How It Was Fixed

<!-- JrePlatform的原理 -->
Tomcat added another class named JrePlatform to solve this problem.

Another component is introduced when fetching a file through URL. The AbstractFileResourceSet will fetch the IS_WINDOWS variable, which would be initialized through the static method in JrePlatform when this class is being loaded.

Then the AbstractFileResourceSet will check if the current platform is windows. If so, the AbstractFileResourceSet will use windows directory naming rule to check the URL passed in. If the validation does pass, AbstractFileResourceSet will simply return null. If the validation passes, the AbstractResourceSet will fetch the real file.

![](https://bloggg-1254259681.cos.na-siliconvalley.myqcloud.com/asiu7.png)


## Extra Work

### Visualization
