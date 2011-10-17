package updown.util


import com.weiglewilczek.slf4s.Logging
import java.io.{IOException, BufferedWriter, File}

object WordleUtils extends Logging {

  val defaultJarPath = "/opt/wordcloud/ibm-word-cloud.jar";
  val defaultConfigurationPath = "/opt/wordcloud/myconf.txt";
  private val _width = 800;
  private val _height = 600;

  def makeWordlesCommand(jarPath: String, configPath: String, source: String, dest: String): String = {
    "java -jar %s -c %s -w %d -h %d -i %s -o %s".format(jarPath, configPath, _width, _height, source, dest)
  }

  def makeWordles(topicFiles: List[String]): List[Process] = makeWordles(defaultJarPath, defaultConfigurationPath, topicFiles)

  def makeWordles(jarPath: String, configPath: String, topicFiles: List[String]): List[Process] =
    makeWordles(jarPath, configPath, topicFiles, None)

  def makeWordles(jarPath: String, configPath: String, topicFiles: List[String], htmlOutputWriter: Option[BufferedWriter]): List[Process] = {
    var children = List[Process]()
    // kick off image generation in parallel
    for (topicFile <- topicFiles) {
      val sourceFile = new File(topicFile);
      if (sourceFile.isFile) {
        val sourcePath = sourceFile.getAbsolutePath
        val destPath = sourcePath + ".png"
        if (htmlOutputWriter.isDefined) {
          htmlOutputWriter.get.write("<div class=wordle><span class=name>%s</span><img src=\"%s\"/></div>".format(sourceFile.getName, destPath))
        }

        logger.debug(sourceFile.getAbsolutePath() + " exists.");
        val command = makeWordlesCommand(jarPath, configPath, sourcePath, destPath);
        logger.debug("Spawning: " + command);
        try {
          children = Runtime.getRuntime().exec(command) :: children
        } catch {
          case s: IOException => logger.error("couldn't launch wordle program. Probably, out of memory.")
        }
      } else {
        logger.error("%s is not a file".format(topicFile))
      }
    }
    logger.debug("exiting makeWordles")
    children
  }

  def waitForChildren(children: List[Process]): Int = {
    var result = 0;
    // wait for all images to be generated before continuing
    for (p <- children) {
      val e = p.waitFor();
      logger.debug("Process output:\n" + scala.io.Source.fromInputStream(p.getErrorStream()).getLines().mkString("\n"))
      result += e;
    }

    result
  }
}
