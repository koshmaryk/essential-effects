name := "essential-effects"

version := "0.1"

scalaVersion := "2.13.6"

val CatsVersion = "2.2.0"
val CatsEffectVersion = "3.2.2"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % CatsEffectVersion,
  "org.typelevel" %% "cats-effect-laws" % CatsEffectVersion % Test
)
