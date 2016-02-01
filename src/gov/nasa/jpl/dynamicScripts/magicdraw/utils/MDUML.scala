/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import java.io.File
import java.lang.Runnable
import javax.swing.filechooser.FileFilter
import javax.swing.{JFileChooser, SwingUtilities}

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.browser.Browser
import com.nomagic.magicdraw.ui.browser.BrowserTabTree
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.uml.BaseElement
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageImport
import com.nomagic.magicdraw.core.options.AbstractPropertyOptionsGroup
import com.nomagic.magicdraw.core.ApplicationEnvironment

import org.apache.log4j.Logger

import org.eclipse.emf.common.util.URI

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ScopeAccess

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.Iterator
import scala.collection.immutable._
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.util.{Failure,Success,Try}
import scala.{AnyVal, Array, Boolean, Int, Option, None, Some, StringContext, Unit}
import scala.Predef.{classOf, refArrayOps, require, String}

case class BrowserNodeElementInfo
( n: Node, e: Element )

case class BrowserTreeSelectionInfo
( p: Project,
  b: BrowserTabTree,
  selection: List[BrowserNodeElementInfo] )

@scala.deprecated("", "")
class MDUML(val p: Project) extends AnyVal {

  def getPrimaryProjectID: String =
    p.getPrimaryProject.getProjectID

  def getProjectLocationURI: URI =
    p.getPrimaryProject.getLocationURI

  def getProjectActiveBrowserTabTree: Option[BrowserTabTree] =
    for {
      b <- Option(p.getBrowser)
      tab <- Option(b.getActiveTree)
    } yield tab

  /**
    * Retrieves the list of selected element nodes in MagicDraw's active browser tab tree, if any.
    *
    * MagicDraw has 5 kinds of browser tab trees: containment, diagrams, inheritance, extensions, search result
    * The MD Open API suggests that the "user object" of a browser tree node should be an MDUML ELement.
    * To avoid doubts, this method filters the nodes to those whose user object is indeed an MDUML Element.
    *
    * @see com.nomagic.magicdraw.ui.browser.Browser
    * @see com.nomagic.magicdraw.ui.browser.BrowserTabTree
    * @see com.nomagic.magicdraw.ui.browser.Node
    */
  def getBrowserTreeSelection: Option[BrowserTreeSelectionInfo] =
    for {
      tab <- getProjectActiveBrowserTabTree
      nodes <- Option(tab.getSelectedNodes)
      nodesWithElements = nodes
        .flatMap { n =>
          n.getUserObject match {
            case e: Element =>
              Some(BrowserNodeElementInfo(n, e))
            case _ =>
              None
          }
        }
        .toList
    } yield BrowserTreeSelectionInfo(p, tab, nodesWithElements)

}

object MDUML {

  implicit def MDUMLHelper(p: Project): MDUML = {
    require(
      null != p,
      "The MDUML helper must be initialized with a valid MagicDraw project")
    new MDUML(p)
  }

  def getMDPluginsLog: Logger =
    MDLog.getPluginsLog

  def getInstallRoot =
    ApplicationEnvironment.getInstallRoot

  def getApplicationInstallDir: File =
    new File(getInstallRoot)

  /**
    *
    */
  def chooseFile
  ( title: String,
    description: String,
    fileNameSuffix: String,
    dir: File = getApplicationInstallDir )
  : Try[Option[File]] =

    Try {
      var result: Option[File] = None

      def chooser = new Runnable {
        override def run(): Unit = {

          val ff = new FileFilter() {

            def getDescription: String = description

            def accept(f: File): Boolean =
              f.isDirectory ||
                (f.isFile && f.getName.endsWith(fileNameSuffix))

          }

          val fc = new JFileChooser(dir) {

            override def getFileSelectionMode: Int = JFileChooser.FILES_ONLY

            override def getDialogTitle = title
          }

          fc.setFileFilter(ff)
          fc.setFileHidingEnabled(true)
          fc.setAcceptAllFileFilterUsed(false)

          fc.showOpenDialog(Application.getInstance().getMainFrame) match {
            case JFileChooser.APPROVE_OPTION =>
              val openFile = fc.getSelectedFile
              result = Some(openFile)
            case _ =>
              result = None
          }
        }
      }

      if (SwingUtilities.isEventDispatchThread)
        chooser.run()
      else
        SwingUtilities.invokeAndWait(chooser)

      result
    }

  /**
    *
    */
  def saveFile
  (title: String,
   description: String,
   fileNameSuffix: String,
   dir: File = getApplicationInstallDir)
  : Try[Option[File]] =
    Try {
      var result: Option[File] = None

      def chooser = new Runnable {
        override def run(): Unit = {

          val ff = new FileFilter() {

            def getDescription: String = description

            def accept(f: File): Boolean =
              f.isDirectory ||
                (f.isFile && f.getName.endsWith(fileNameSuffix))

          }

          val fc = new JFileChooser(dir) {

            override def getFileSelectionMode: Int = JFileChooser.FILES_ONLY

            override def getDialogTitle = title
          }

          fc.setFileFilter(ff)
          fc.setFileHidingEnabled(true)
          fc.setAcceptAllFileFilterUsed(false)

          fc.showSaveDialog(Application.getInstance().getMainFrame) match {
            case JFileChooser.APPROVE_OPTION =>
              val saveFile = fc.getSelectedFile
              result = Some(saveFile)
            case _ =>
              result = None
          }
        }
      }

      if (SwingUtilities.isEventDispatchThread)
        chooser.run()
      else
        SwingUtilities.invokeAndWait(chooser)

      result
    }

  def getProjectLocationURI( p: Project ): URI =
    p.getProjectLocationURI
  
  def getPropertyOfOptionsGroup( g: AbstractPropertyOptionsGroup, propertyID: String ) =
    g.getProperty( propertyID )
    
  case class ActiveDiagramSelectionInfo
  ( p: Project,
    d: DiagramPresentationElement,
    selected: java.util.List[PresentationElement] )

  /**
   * Retrieves the list of selected symbols (shapes, paths) of any kind in MagicDraw's active diagram, if any.
   */
  def getActiveDiagramSelection: Option[ActiveDiagramSelectionInfo] =
    Application.getInstance().getProject match {
      case null =>
        None
      case p: Project =>
        p.getActiveDiagram match {
          case null =>
            None
          case d: DiagramPresentationElement =>
            Some(ActiveDiagramSelectionInfo(p, d, d.getSelected))
        }
    }

  def elementPackageContainmentIterator( e: Element ): Iterator[Package] = new Iterator[Package] {
    private var p = ModelHelper.findParentOfType( e, classOf[Package] )

    def hasNext: Boolean = p != null

    def next: Package = {
      val result = p
      p = p.getNestingPackage
      result
    }
  }

  def getAllImportedPackages( p: Package ): Set[Package] = {

    def collectImportedPackages( imports: List[PackageImport], imported: Set[Package] ): Set[Package] =
      imports match {
        case Nil => imported
        case x :: xs =>
          val ip = x.getImportedPackage
          if ( imported.contains( ip ) ) collectImportedPackages( xs, imported )
          else collectImportedPackages( ip.getPackageImport.toList ++ xs, imported + ip )
      }

    if ( p == null ) Set()
    else collectImportedPackages( p.getPackageImport.toList, Set() )
  }

  def getAllGeneralClassifiersIncludingSelf( cls: Classifier ): List[Classifier] = {

    def getAllGeneralClassifiers( cls: Classifier ): List[Classifier] = {
      val generalClassifiers: java.util.List[Classifier] = new java.util.ArrayList[Classifier]()
      ModelHelper.collectGeneralClassifiersRecursivelly( generalClassifiers, cls )
      generalClassifiers.toList
    }

    if ( cls == null ) List()
    else List( cls ) ++ getAllGeneralClassifiers( cls )
  }

  def isAccessCompatibleWithElements( access: ScopeAccess.Value, elements: BaseElement* ): Boolean = {
    val mustBeEditable = access match {
      case ScopeAccess.READ_ONLY  => false
      case ScopeAccess.READ_WRITE => true
    }
    val compatibleAccess = elements.forall { e => !mustBeEditable || e.isEditable }
    compatibleAccess
  }
}