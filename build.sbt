name := "essential-effects"

version := "0.1"

scalaVersion := "2.13.6"

val CatsVersion = "2.5.4"
val CatsEffectVersion = "3.3.11"

libraryDependencies ++= Seq(
  //"org.typelevel" %% "cats-effect" % CatsEffectVersion,
  "org.typelevel" %% "cats-effect" % CatsVersion,
  "org.typelevel" %% "cats-effect-laws" % CatsEffectVersion % Test
)
