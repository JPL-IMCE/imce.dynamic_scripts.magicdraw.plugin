/*
 * Copyright 2014 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * License Terms
 */

package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import java.io.File
import java.lang.{Runnable, System}
import java.net.URL
import java.nio.file.{Path, Paths}
import javax.swing.filechooser.FileFilter
import javax.swing.{JFileChooser, SwingUtilities}

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.core.modules.ModulesServiceInternal
import com.nomagic.magicdraw.core.project.ProjectsManager
import com.nomagic.magicdraw.core.utils.ChangeElementID
import com.nomagic.magicdraw.ui.browser.BrowserTabTree
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.uml.BaseElement
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.task.ProgressStatus
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageImport
import com.nomagic.magicdraw.core.options.AbstractPropertyOptionsGroup
import com.nomagic.magicdraw.core.ApplicationEnvironment
import com.nomagic.ui.{ResizableIcon, SwingImageIcon}
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper
import org.apache.log4j.Logger
import org.eclipse.emf.common.util.URI
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ScopeAccess

import scala.collection.JavaConversions.{asJavaCollection, mapAsJavaMap}
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.Iterator
import scala.collection.immutable._
import scala.language.implicitConversions
import scala.util.Try
import scala.{AnyVal, Boolean, Int, None, Option, Some, Unit}
import scala.Predef.{String, classOf, refArrayOps, require}
import scala.reflect.ClassTag

case class BrowserNodeElementInfo
( n: Node, e: Element )

case class BrowserTreeSelectionInfo
( p: Project,
  b: BrowserTabTree,
  selection: List[BrowserNodeElementInfo] )

@scala.deprecated("", "")
class MDUML(@scala.transient val p: Project) extends AnyVal {


  def enableResettingIDs(): Boolean = {
    val mdCounter = p.getCounter
    val flag = mdCounter.canResetIDForObject
    mdCounter.setCanResetIDForObject( true )
    flag
  }

  def changeElementIDs
  ( elements: Set[Element],
    old2newIDMap: Map[String, String],
    progressStatus: ProgressStatus )
  : Unit = {
    ChangeElementID.resetIDS( p, elements, old2newIDMap, progressStatus, false )
  }

  def restoreResettingIDs(flag: Boolean): Unit = {
    val mdCounter = p.getCounter
    mdCounter.setCanResetIDForObject( flag )
  }

  def getProjectDirectory: Option[File] =
    p.getDirectory match {
      case null =>
        None
      case "" =>
        None
      case d =>
        Some(new File(d))
    }

  def getPrimaryProjectID: String =
    p.getPrimaryProject.getProjectID

  def getProjectLocationURI: URI =
    p.getPrimaryProject.getLocationURI

  /**
    * Open the MagicDraw Module wizard prompting the user to mount a local module to the project.
    *
    * This internal API facilitates a one-time migration of a local module according to an ID mapping table.
    * Note that this one-time migration does not involve MagicDraw's "alias.properties" ID-mapping mechanism,
    * which imposes an overhead for all projects/modules that are opened or saved.
    *
    * @return True if the user performed an operation & closed the wizard; false if the user cancelled the wizard
    */
  def promptUseLocalModuleWithWizard: Boolean =
    ModulesServiceInternal.useLocalModuleWithWizard( p.getPrimaryProject )

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

  def findElementWithPath[T <: Element : ClassTag](qname: String, metaclass: java.lang.Class[_ <: T]): Option[T]
  = Option.apply(ModelHelper.findElementWithPath(p, qname, metaclass)) match {
    case Some(e: T) =>
      Some(e)
    case _ =>
      None
  }

}

object MDUML {

  def createResizableIconFromURL(url: URL): ResizableIcon = {
    new SwingImageIcon( url )
  }

  implicit def MDUMLHelper(p: Project): MDUML = {
    require(
      null != p,
      "The MDUML helper must be initialized with a valid MagicDraw project")
    new MDUML(p)
  }

  def getRecentFilePathOrUserDirectory: Path = {
    ProjectsManager.getRecentFilePath match {
      case _@ ( null | "" ) =>
        System.getProperty( "user.dir" ) match {
          case _@ ( null | "" ) =>
            Paths.get(".").toAbsolutePath
          case d                =>
            new File(d).toPath
        }
      case d =>
        new File(d).toPath
    }
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

  /**
    * Corresponds to the MD 18.0 Open API: ModelHelper.findParentOfType
    */
  final def findParentOfType[T <: Element : ClassTag](e: Element, metaClass: java.lang.Class[T]): Option[T]
  = Option.apply(e.getOwner) match {
      case Some(o: T) =>
        Some(o)
      case Some(o) =>
        findParentOfType(o, metaClass)
      case None =>
        None
    }

  def elementPackageContainmentIterator( e: Element ): Iterator[Package] = new Iterator[Package] {

    private var p = findParentOfType( e, classOf[Package] )

    def hasNext: Boolean = p.nonEmpty

    def next: Package = {
      val result = p.get
      p = Option.apply(result.getNestingPackage)
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

  def getAllGeneralClassifiersIncludingSelf( cls: Classifier ): Set[Classifier] = {

    def getAllGeneralClassifiers( queue: Set[Classifier], acc: Set[Classifier] )
    : Set[Classifier]
    = if (queue.isEmpty) acc
    else {
      val (h, t) = (queue.head, queue.tail)
      val gs = h.getGeneral.to[Set]
      val inc = gs -- acc
      getAllGeneralClassifiers(t ++ inc, acc ++ gs + h)
    }

    if ( null == cls ) Set()
    else getAllGeneralClassifiers( Set(cls), Set.empty )
  }

  def isAccessCompatibleWithElements( access: ScopeAccess.Value, elements: BaseElement* ): Boolean = {
    val mustBeEditable = access match {
      case ScopeAccess.READ_ONLY  => false
      case ScopeAccess.READ_WRITE => true
    }
    val compatibleAccess = elements.forall { e => !mustBeEditable || e.isEditable }
    compatibleAccess
  }

  def findElementWithPath[T <: Element: ClassTag](p: Project, qname: String, metaclass: java.lang.Class[_ <: T]): Option[T]
  = p.findElementWithPath(qname, metaclass)
}