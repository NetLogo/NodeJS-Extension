import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension)
enablePlugins(ExtensionDocumentationPlugin)

version    := "0.1.0"
isSnapshot := true

netLogoVersion       := "6.2.2"
netLogoClassManager  := "org.nlogo.extensions.js.JSExtension"
netLogoExtName       := "js"
netLogoPackageExtras += (baseDirectory.value / "src" / "jsext.js", None)

scalaVersion           := "2.12.12"
scalaSource in Test    := baseDirectory.value / "src" / "test"
scalaSource in Compile := baseDirectory.value / "src" / "main"
scalacOptions         ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")

resolvers           += "netlogo-lang-extension" at "https://dl.cloudsmith.io/public/netlogo/netlogoextensionlanguageserverlibrary/maven"
libraryDependencies ++= Seq(
  "org.json4s"              %% "json4s-jackson"     % "3.5.3"
, "org.nlogo.langextension" %% "lang-extension-lib" % "0.3"
)
