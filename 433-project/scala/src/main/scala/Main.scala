object Main {
  def main(args: Array[String]): Unit = {
    if (args(0) == "master")
      master()
    else
      slave()
  }

  def master(): Unit = {
    Master.main(Array())
  }

  def slave(): Unit = {
    Slave.main(Array())
  }
}