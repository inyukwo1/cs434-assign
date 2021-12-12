object Main {
  def main(args: Array[String]): Unit = {
    if (args(0) == "master")
      master(args)
    else
      slave(args)
  }

  def master(args: Array[String]): Unit = {
    Master.main_(args)
  }

  def slave(args: Array[String]): Unit = {
    Slave.main_(args)
  }
}