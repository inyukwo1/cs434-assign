name := "scala"

version := "0.1"

scalaVersion := "2.12.10"

packageName in Docker := "cs434project"

mainClass in Compile := Some("Main")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
)
