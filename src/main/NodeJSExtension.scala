package org.nlogo.extensions.js

import java.io.File

import com.fasterxml.jackson.core.JsonParser
import org.json4s.jackson.JsonMethods.mapper

import org.nlogo.languagelibrary.Subprocess
import org.nlogo.languagelibrary.config.{ Config, Menu }
import org.nlogo.api
import org.nlogo.api._
import org.nlogo.core.Syntax

// All the state is stored within the subprocess the subprocess itself is stored at the object (static in Java
// terminology) level.
object NodeJSExtension {
  val codeName   = "js"
  val longName   = "NodeJS Extension"
  val extLangBin = "node"

  var menu: Option[Menu] = None
  val config: Config     = Config.createForPropertyFile(classOf[NodeJSExtension], NodeJSExtension.codeName)

  private var _nodeProcess: Option[Subprocess] = None

  // Get nodejs process, fail if not started yet
  def nodeProcess: Subprocess =
    _nodeProcess.getOrElse(throw new ExtensionException((
      "Node.JS Process has not been started. Please run js:setup first before any other js extension primitive"
    )))

  // Set the current nodejs process, closing the prior one if need be
  // This is a Scala setter method that automatically gets called when you write NodeJSExtension.nodeProcess = <something>
  // See https://www.oreilly.com/library/view/scala-cookbook/9781449340292/ch04s07.html for more
  def nodeProcess_=(proc: Subprocess): Unit = {
    _nodeProcess.foreach(_.close())
    _nodeProcess = Some(proc)
  }

  // Kill the nodejs process
  def killNode(): Unit = {
    _nodeProcess.foreach(_.close())
    _nodeProcess = None
  }

}

// The extension manager itself. Handles creating and executing the extension's primitives.
class NodeJSExtension extends DefaultClassManager {
  def load(manager: PrimitiveManager): Unit = { // add all this extension's primitives
    manager.addPrimitive("setup", SetupNode)
    manager.addPrimitive("run", Run)
    manager.addPrimitive("runresult", RunResult)
    manager.addPrimitive("set", Set)
  }

  // One time init for this extension.
  override def runOnce(em: ExtensionManager): Unit = {
    super.runOnce(em)
    mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true) // configure json parser

    NodeJSExtension.menu = Menu.create(NodeJSExtension.longName, NodeJSExtension.extLangBin, NodeJSExtension.config)
  }

  // Teardown on extension unload
  override def unload(em: ExtensionManager): Unit = {
    super.unload(em)
    NodeJSExtension.killNode() // Kill the subprocess
    NodeJSExtension.menu.foreach(_.unload()) // remove the menu bar item
  }
}

object SetupNode extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List())

  override def perform(args: Array[Argument], context: Context): Unit = {
    val nodeJsExtensionDirectory = Config.getExtensionRuntimeDirectory(classOf[NodeJSExtension], NodeJSExtension.codeName)
    val maybeJsFile              = new File(nodeJsExtensionDirectory, "jsext.js")
    val jsFile                   = if (maybeJsFile.exists) { maybeJsFile } else { (new File("jsext.js")).getCanonicalFile }
    val jsScript                 = jsFile.toString

    val maybeJsRuntimePath   = Config.getRuntimePath(
        NodeJSExtension.extLangBin
      , NodeJSExtension.config.runtimePath.getOrElse("")
      , "--version"
    )
    val jsRuntimePath = maybeJsRuntimePath.getOrElse(
      throw new ExtensionException(s"We couldn't find a Node.js executable file to run.  Please make sure Node.js is installed on your system.  Then you can tell the ${NodeJSExtension.longName} where it's located by opening the NodeJS Extension menu and selecting Configure to choose the location yourself or putting making sure ${NodeJSExtension.extLangBin} is available on your PATH.\n")
    )

    try {
      // Wipe the slate clean
      NodeJSExtension.killNode()
      // Start the subprocess with the current workspace, the executable to be run, any args to be passed in,
      // and the name of the extension, and a longer name for the extension, both for more helpful error messages
      NodeJSExtension.nodeProcess = Subprocess.start(context.workspace, Seq(jsRuntimePath), Seq(jsScript), "js", "Node.js Javascript")
      // Set the method that the shell window will call to evaluate expressions. In this case, we just go straight
      // to the evalStringified method of the subprocess
      NodeJSExtension.menu.foreach(_.setup(NodeJSExtension.nodeProcess.evalStringified))
    } catch {
      case e: Exception => {
        throw new ExtensionException("The subprocess didn't want to start", e)
      }
    }
  }
}

object Run extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType | Syntax.RepeatableType) // This tells NetLogo that you should accept variadic input
                                                            // e.g. (js:run "console.log(1)" "console.log(2)")
  )

  override def perform(args: Array[Argument], context: Context): Unit =
    NodeJSExtension.nodeProcess.exec(args.map(_.getString).mkString("\n"))
}

object RunResult extends api.Reporter {
  override def getSyntax: Syntax = Syntax.reporterSyntax(
    right = List(Syntax.StringType),
    ret = Syntax.WildcardType
  )

  override def report(args: Array[Argument], context: Context): AnyRef =
    NodeJSExtension.nodeProcess.eval(args.map(_.getString).mkString("\n"))
}

object Set extends api.Command {
  // Subprocess.convertibleTypesSyntax is all of the things that the library knows how to interpret, including
  // agents, agentSets, and everything in Syntax.ReadableType
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List(Syntax.StringType, Subprocess.convertibleTypesSyntax))
  override def perform(args: Array[Argument], context: Context): Unit =
    NodeJSExtension.nodeProcess.assign(args(0).getString, args(1).get)
}
