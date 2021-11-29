enablePlugins(org.nlogo.build.NetLogoExtension)

resolvers      += "netlogo" at "https://dl.cloudsmith.io/public/netlogo/netlogo/maven/"
netLogoVersion := "6.2.0-d27b502"

netLogoClassManager := "org.nlogo.extensions.js.JSExtension"

version := "0.1.0-SNAPSHOT"

isSnapshot := true

netLogoExtName := "js"

netLogoZipSources := false

netLogoTarget := org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value)

scalaVersion := "2.12.12"

scalaSource in Test := baseDirectory.value / "src" / "test"

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "org.json4s"        %% "json4s-jackson" % "3.5.3",
  "org.nlogo.langextension" %% "lang-extension-lib" % "0.1-SNAPSHOT",
)

netLogoPackageExtras += (baseDirectory(_ / "src" / "jsext.js").value, "jsext.js")

