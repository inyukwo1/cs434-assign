name := "scala"

version := "0.1"

scalaVersion := "2.12.10"

Docker / packageName := "cs434project"

Compile / mainClass := Some("Main")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.10"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"