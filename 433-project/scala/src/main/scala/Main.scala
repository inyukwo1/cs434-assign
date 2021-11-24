object Main {
  def main(args: Array[String]): Unit = {
    if (args(0) == "master")
      println("master")
    else
      println("slave")
  }
}