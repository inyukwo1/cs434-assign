name := "scala"

version := "0.1"

scalaVersion := "2.13.7"

packageName in Docker := "cs434project"

mainClass in Compile := Some("Main")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)