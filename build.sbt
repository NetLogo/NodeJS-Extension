import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(ExtensionDocumentationPlugin, NetLogoExtension)

name       := "NodeJS Extension"
version    := "0.1.2"
isSnapshot := true

scalaVersion           := "2.12.12"
scalaSource in Test    := baseDirectory.value / "src" / "test"
scalaSource in Compile := baseDirectory.value / "src" / "main"
scalacOptions         ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint")

netLogoVersion       := "6.2.2"
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
  "org.nlogo.languagelibrary" %% "language-library" % "2.0.0"
)
