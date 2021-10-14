package org.nlogo.extensions.js

import com.fasterxml.jackson.core.JsonParser
import org.json4s.jackson.JsonMethods.mapper
import org.me.{ShellWindow, Subprocess}
import org.nlogo.api
import org.nlogo.api._
import org.nlogo.app.App
import org.nlogo.core.Syntax

import java.awt.GraphicsEnvironment
import java.io.File
import javax.swing.JMenu

object JSExtension {
  private var _nodeProcess: Option[Subprocess] = None
  var shellWindow : Option[ShellWindow] = None

  val extDirectory: File = new File(
    getClass.getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs()(0).toURI.getPath
  ).getParentFile

  def nodeProcess: Subprocess =
    _nodeProcess.getOrElse(throw new ExtensionException((
      "Node.JS Process has not been started. Please run js:setup first before any other js extension primitive"
    )))

  def nodeProcess_=(proc: Subprocess): Unit = {
    _nodeProcess.foreach(_.close())
    _nodeProcess = Some(proc)
  }

  def killNode(): Unit = {
    _nodeProcess.foreach(_.close())
    _nodeProcess = None
  }
}

class JSExtension extends DefaultClassManager {
  var extensionMenu: Option[JMenu] = None

  def load(manager: PrimitiveManager): Unit = {
    manager.addPrimitive("setup", SetupNode)
    manager.addPrimitive("run", Run)
    manager.addPrimitive("runresult", RunResult)
    manager.addPrimitive("set", Set)
  }

  override def runOnce(em: ExtensionManager): Unit = {
    super.runOnce(em)
    mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)

    if (!GraphicsEnvironment.isHeadless) {
      JSExtension.shellWindow = Some(new ShellWindow())

      val menuBar = App.app.frame.getJMenuBar

      menuBar.getComponents.collectFirst {
        case mi: JMenu if mi.getText == ExtensionMenu.name => mi
      }.getOrElse {
        extensionMenu = Option(menuBar.add(new ExtensionMenu))
      }
    }
  }

  override def unload(em: ExtensionManager): Unit = {
    super.unload(em);
    JSExtension.killNode()
    JSExtension.shellWindow.foreach(sw => sw.setVisible(false))
    if (!GraphicsEnvironment.isHeadless) {
      extensionMenu.foreach(App.app.frame.getJMenuBar.remove(_))
    }
  }
}

object SetupNode extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List())

  override def perform(args: Array[Argument], context: Context): Unit = {
    val jsScript: String = new File(JSExtension.extDirectory, "jsext.js").toString
    try {
      JSExtension.killNode()
      JSExtension.nodeProcess = Subprocess.start(context.workspace, Seq("node"), Seq(jsScript), "js", "Node.js Javascript")
      JSExtension.shellWindow.foreach(sw => sw.eval_stringified = Some(JSExtension.nodeProcess.evalStringified))
    } catch {
      case e: Exception => {
        println(e)
        throw new ExtensionException("didn't want to start")
      }
    }
  }
}

object Run extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType | Syntax.RepeatableType)
  )

  override def perform(args: Array[Argument], context: Context): Unit =
    JSExtension.nodeProcess.exec(args.map(_.getString).mkString("\n"))
}

object RunResult extends api.Reporter {
  override def getSyntax: Syntax = Syntax.reporterSyntax(
    right = List(Syntax.StringType | Syntax.RepeatableType),
    ret = Syntax.WildcardType
  )

  override def report(args: Array[Argument], context: Context): AnyRef =
    JSExtension.nodeProcess.eval(args.map(_.getString).mkString("\n"))
}

object Set extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List(Syntax.StringType, Syntax.ReadableType))
  override def perform(args: Array[Argument], context: Context): Unit =
    JSExtension.nodeProcess.assign(args(0).getString, args(1).get)
}

object ExtensionMenu {
  val name = "JSExtension"
}

class ExtensionMenu extends JMenu("JSExtension") {
  add("Pop-out Interpreter").addActionListener{ _ =>
    JSExtension.shellWindow.map(sw => sw.setVisible(!sw.isVisible))
  }
}