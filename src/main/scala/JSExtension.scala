package org.nlogo.extensions.js

import com.fasterxml.jackson.core.JsonParser
import org.json4s.jackson.JsonMethods.mapper
import org.nlogo.langextension.{ShellWindow, Subprocess}
import org.nlogo.api
import org.nlogo.api._
import org.nlogo.app.App
import org.nlogo.core.Syntax

import java.awt.GraphicsEnvironment
import java.io.File
import javax.swing.JMenu

// All the state is stored within the subprocess the subprocess itself is stored at the object (static in Java
// terminology) level.
object JSExtension {
  private var _nodeProcess: Option[Subprocess] = None
  var shellWindow : Option[ShellWindow] = None

  // The directory where the .js file is located
  val extDirectory: File = new File(
    getClass.getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs()(0).toURI.getPath
  ).getParentFile

  // Get nodejs process, fail if not started yet
  def nodeProcess: Subprocess =
    _nodeProcess.getOrElse(throw new ExtensionException((
      "Node.JS Process has not been started. Please run js:setup first before any other js extension primitive"
    )))

  // Set the current nodejs process, closing the prior one if need be
  // This is a Scala setter method that automatically gets called when you write JSExtension.nodeProcess = <something>
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
class JSExtension extends DefaultClassManager {
  var extensionMenu: Option[JMenu] = None // The menu bar item for this extension

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

    if (!GraphicsEnvironment.isHeadless) {
      JSExtension.shellWindow = Some(new ShellWindow()) // Create, but do not show, shell window. The ShellWindow
                                                        // is part of the library and is documented there

      // Try to find a menu item with the right name
      val menuBar = App.app.frame.getJMenuBar
      val maybeMenuItem = menuBar.getComponents.collectFirst{
        case mi: JMenu if mi.getText == ExtensionMenu.name => mi
      }
      // If unable to find one, create a new menu bar item
      if (maybeMenuItem.isEmpty) {
        extensionMenu = Option(menuBar.add(new ExtensionMenu))
      }
    }
  }

  // Teardown on extension unload
  override def unload(em: ExtensionManager): Unit = {
    super.unload(em);
    JSExtension.killNode() // Kill the subprocess
    JSExtension.shellWindow.foreach(sw => sw.setVisible(false)) // hide the shell window
    if (!GraphicsEnvironment.isHeadless) {
      extensionMenu.foreach(menu => App.app.frame.getJMenuBar.remove(menu)) // remove the menu bar item
    }
  }
}

object SetupNode extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List())

  override def perform(args: Array[Argument], context: Context): Unit = {
    val jsScript: String = new File(JSExtension.extDirectory, "jsext.js").toString
    try {
      // Wipe the slate clean
      JSExtension.killNode()
      // Start the subprocess with the current workspace, the executable to be run, any args to be passed in,
      // and the name of the extension, and a longer name for the extension, both for more helpful error messages
      JSExtension.nodeProcess = Subprocess.start(context.workspace, Seq("node"), Seq(jsScript), "js", "Node.js Javascript")
      // Set the method that the shell window will call to evaluate expressions. In this case, we just go straight
      // to the evalStringified method of the subprocess
      JSExtension.shellWindow.foreach(sw => sw.setEvalStringified(Some(JSExtension.nodeProcess.evalStringified)))
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
    JSExtension.nodeProcess.exec(args.map(_.getString).mkString("\n"))
}

object RunResult extends api.Reporter {
  override def getSyntax: Syntax = Syntax.reporterSyntax(
    right = List(Syntax.StringType),
    ret = Syntax.WildcardType
  )

  override def report(args: Array[Argument], context: Context): AnyRef =
    JSExtension.nodeProcess.eval(args.map(_.getString).mkString("\n"))
}

object Set extends api.Command {
  // Subprocess.convertibleTypesSyntax is all of the things that the library knows how to interpret, including
  // agents, agentSets, and everything in Syntax.ReadableType
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List(Syntax.StringType, Subprocess.convertibleTypesSyntax))
  override def perform(args: Array[Argument], context: Context): Unit =
    JSExtension.nodeProcess.assign(args(0).getString, args(1).get)
}

object ExtensionMenu {
  val name = "JSExtension"
}

// The extension menu bar item. In this case, it just has the one drop down item, toggling the interactive JS console.
class ExtensionMenu extends JMenu("JSExtension") {
  add("Interactive JS Console").addActionListener{ _ =>
    JSExtension.shellWindow.map(sw => sw.setVisible(!sw.isVisible))
  }
}
