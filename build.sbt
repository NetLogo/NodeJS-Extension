import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(ExtensionDocumentationPlugin, NetLogoExtension)

name       := "NodeJS Extension"
version    := "0.3.0"
isSnapshot := true

scalaVersion          := "3.7.0"

Test / scalaSource    := baseDirectory.value / "src" / "test"
Compile / scalaSource := baseDirectory.value / "src" / "main"
scalacOptions        ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-release", "17")

netLogoVersion       := "7.0.0-beta2-4fe3739" // This extension gets its NL version from language-library; any update to its NL version has to be mirrored in that package and published in a new version --Jason B. (7/1/25)
netLogoClassManager  := "org.nlogo.extensions.js.NodeJSExtension"
netLogoExtName       := "js"
netLogoPackageExtras += (baseDirectory.value / "src" / "jsext.js", None)
netLogoZipExtras    ++= Seq(baseDirectory.value / "demos", baseDirectory.value / "README.md")

Compile / packageBin / artifactPath := {
  val oldPath = (Compile / packageBin / artifactPath).value.toPath
  val newPath = oldPath.getParent / s"${netLogoExtName.value}.jar"
  newPath.toFile
}

resolvers           += "netlogo-language-library" at "https://dl.cloudsmith.io/public/netlogo/language-library/maven"
libraryDependencies ++= Seq(
  "org.nlogo.languagelibrary" %% "language-library" % "3.3.0"
)
