name := "essential-effects"

version := "0.1"

scalaVersion := "2.13.6"

val CatsEffect2Version = "2.5.4"
val CatsEffect3Version = "3.3.11"
val mapRefVersion = "0.2.1"

libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "mapref" % mapRefVersion,
  "org.typelevel" %% "cats-effect" % CatsEffect3Version,
  "org.typelevel" %% "cats-effect-laws" % CatsEffect3Version % Test
)
