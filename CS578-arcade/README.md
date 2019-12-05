# CS578-arcade
Clone of arcade, to be used in HW3 for CS578, fall 2019, at USC
To run the JAR files, java8 is needed. 
To generate JAR files: 

Exporting a JAR: From Eclipse, export one JAR for each method within ARCADE that you would like to run as your main method. 
Example for BatchClusteringEngine (ARC):
a) Open  any Launch configuration that contains BatchClusteringEngine:
<ol>
      
      
      
      1. Change "<listEntry value="/arcade/src/edu/usc/softarch/arcade/AcdcWithSmellDetection.java"/>" to 
      "<listEntry value="/CS578-arcade/src/edu/usc/softarch/arcade/AcdcWithSmellDetection.java"/>" 
      
      2. Change "<stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="arcade"/>" to 
      <stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="CS578-arcade"/>  
      
      3. Save the file 
 </ol>
      
b) Right-click on the “arcade” project.

c) Export-> Java -> Runnable JAR file, then click “Next”

d) Select any Launch configuration that contains “BatchClusteringEngine” (it doesn’t matter which one because the parameters will be stripped out).

e) Select an export destination file name.

f) Leave “Library handling:” as is.

g) Click “Finish”.

There will probably be an alert about repacking referenced libraries and library licenses. Click “OK”.

There may be an alert saying “JAR export finished with warnings”. This is due to numerous warnings in ARCADE (that are also shown in Eclipse) and is okay. Click “OK”.
