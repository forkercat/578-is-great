package edu.usc.softarch.arcade;

import OurSolution.Tomdog;
import acdc.ACDC;
import com.google.common.base.Joiner;
import edu.usc.softarch.arcade.antipattern.detection.ArchSmellDetector;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.CSourceToDepsBuilder;
import edu.usc.softarch.arcade.facts.driver.JavaSourceToDepsBuilder;
import edu.usc.softarch.arcade.facts.driver.SourceToDepsBuilder;
import edu.usc.softarch.arcade.util.FileUtil;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class AcdcWithSmellDetection {

  static Logger logger = Logger.getLogger(AcdcWithSmellDetection.class);

  public static void main(String[] args) throws IOException {
    System.out.println("finally compiled...");
    PropertyConfigurator.configure(Config.getLoggingConfigFilename());

    // inputDirName is a directory where each subdirectory contains a revision or version of the system to be analyzed
    String inputDirName = args[0];
    File inputDir = new File(FileUtil.tildeExpandPath(inputDirName));

    // outputDirName is the directory where dependencies rsf files, cluster rsf files, and detected smells ser files are generated
    String outputDirName = args[1];
    File outputDir = new File(FileUtil.tildeExpandPath(outputDirName));

    File[] files = inputDir.listFiles();

    Set<File> fileSet = new TreeSet<File>(Arrays.asList(files));

	  System.out.println(fileSet);

    logger.debug("All files in " + inputDir + ":");
    logger.debug(Joiner.on("\n").join(fileSet));
    for (File file : fileSet) {
      if (file.isDirectory()) {
        logger.debug("Identified directory: " + file.getName());
      }
    }
    for (File vFolder : fileSet) {
      if (vFolder.isDirectory()) {
	      System.out.println("+++main+++" + vFolder);
        single(vFolder, args, outputDir);
      }
    }
    System.out.println("Finished all... bye :(");
  }


  public static void single(File versionFolder, String[] args, File outputDir) throws FileNotFoundException, IOException {
    logger.debug("Processing directory: " + versionFolder.getName());
    // the revision number is really just the name of the subdirectory, for hadoop I actually name each subdirectory based on the revision number
    String revisionNumber = versionFolder.getName();

    // classesDir is the directory in each subdirectory of the dir directory that contains the compiled classes of the subdirectory
    String classesDir = args[2];
    String absoluteClassesDir = versionFolder.getAbsolutePath() + File.separatorChar + classesDir;
    File classesDirFile = new File(absoluteClassesDir);
    if (!classesDirFile.exists())
      return;

    // depsRsfFilename is the file name of the dependencies rsf file (one is created per subdirectory of dir)
    String depsRsfFilename = outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_deps.rsf";
    String[] builderArgs = { absoluteClassesDir, depsRsfFilename }; /** output files */
    File depsRsfFile = new File(depsRsfFilename);
    if (!depsRsfFile.getParentFile().exists())
      depsRsfFile.getParentFile().mkdirs();

    logger.debug("Get deps for revision " + revisionNumber);

    SourceToDepsBuilder builder = new JavaSourceToDepsBuilder();
    if (args.length == 4) {
      if (args[3].equals("c")) {
        builder = new CSourceToDepsBuilder();
      }
    }

    /** Build Dependencies */
    if (true) {
      // Initialize Tomdog
      String path = "../tomcat/src/8.5.47/"; // project path (subject to change)
      Tomdog.setProjectPath(path);
      Tomdog.getInstanceDog(); // singleton (calling this method initializes the Tomdog object)

      // Inside this method, we add our solution that helps find more dependencies.
      builder.build(builderArgs); /** this line builds the dependency in .rsf file */

      System.out.println("\nFinish building dependencies... :)");

      // if (true) return; // for debugging building dependencies

      if (builder.getEdges().size() == 0) {
        return;
      }
    }

    /** ACDC */
    System.out.println("\nStarting ACDC...");
    // acdcClusteredfile is the recovered architecture for acdc, one per subdirectory of dir
    String acdcClusteredFile = outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_acdc_clustered.rsf";
    String[] acdcArgs = {depsRsfFile.getAbsolutePath(), acdcClusteredFile};

	  System.out.println("ardcArgs: " + Arrays.toString(acdcArgs));

    System.out.println("Running acdc for revision    please wait T_T " + revisionNumber);
    ACDC.main(acdcArgs);

    // the last element of the smellArgs array is the location of the file containing the detected smells (one is created per subdirectory of dir)
    String[] smellArgs = {depsRsfFile.getAbsolutePath(), acdcClusteredFile, outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_acdc_smells.ser"};
    logger.debug("Running smell detecion for revision " + revisionNumber);

    /** Smell Detector */
    System.out.println("\nDetecting Smells...\n");
    ArchSmellDetector.setupAndRunStructuralDetectionAlgs(smellArgs);

    System.out.println("\nFinish ACDC... :)\n");

  }
}
