import java.io.File

object TypeSystemTester {
  def main(args:Array[String]): Unit = {
    val folder = new File("testFiles/parsing")
    val testFiles = folder.listFiles().sorted
    testFiles.foreach(p => {
      println(p.getName)
      Main.main(Array(p.getPath))
      println("-----------------------------------------------------------------")
    })
  }
}
