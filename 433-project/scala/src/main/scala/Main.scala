object Main {
  def main(args: Array[String]): Unit = {
    if (args(0) == "master")
      master(args)
    else
      slave(args)
  }

  def master(args: Array[String]): Unit = {
    Master.main(args)
  }

  def slave(args: Array[String]): Unit = {
    Slave.main(args)
  }
}