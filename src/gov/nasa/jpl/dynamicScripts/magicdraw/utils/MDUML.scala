/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.language.implicitConversions
import scala.language.postfixOps
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.browser.Browser
import com.nomagic.magicdraw.ui.browser.BrowserTabTree
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.uml.BaseElement
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageImport
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ScopeAccess
import com.nomagic.magicdraw.core.options.AbstractPropertyOptionsGroup
import com.nomagic.magicdraw.core.ApplicationEnvironment

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object MDUML {

  def getInstallRoot() = ApplicationEnvironment.getInstallRoot
  
  def getProjectLocationURI( p: Project ) = p.getPrimaryProject.getLocationURI
  
  def getPropertyOfOptionsGroup( g: AbstractPropertyOptionsGroup, propertyID: String ) =
    g.getProperty( propertyID )
    
  case class ActiveDiagramSelectionInfo( p: Project, d: DiagramPresentationElement, selected: java.util.List[PresentationElement] )

  /**
   * Retrieves the list of selected symbols (shapes, paths) of any kind in MagicDraw's active diagram, if any.
   */
  def getActiveDiagramSelection(): Option[ActiveDiagramSelectionInfo] =
    Application.getInstance().getProject() match {
      case null => None
      case p: Project => p.getActiveDiagram() match {
        case null                          => None
        case d: DiagramPresentationElement => Some( ActiveDiagramSelectionInfo( p, d, d.getSelected() ) )
      }
    }

  case class BrowserNodeElementInfo( n: Node, e: Element )

  case class BrowserTreeSelectionInfo( p: Project, b: BrowserTabTree, selection: List[BrowserNodeElementInfo] )

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
  def getBrowserTreeSelection(): Option[BrowserTreeSelectionInfo] =
    Application.getInstance().getProject() match {
      case null => None
      case p: Project => p.getBrowser() match {
        case null => None
        case b: Browser => b.getActiveTree() match {
          case null => None
          case tab: BrowserTabTree => tab.getSelectedNodes() match {
            case null =>
              None
            case nodes: Array[Node] =>
              val nodesWithElements = nodes flatMap ( n => n.getUserObject() match {
                case e: Element => Some( BrowserNodeElementInfo( n, e ) )
                case _          => None
              } ) toList;
              Some( BrowserTreeSelectionInfo( p, tab, nodesWithElements ) )
          }
        }
      }
    }

  def elementPackageContainmentIterator( e: Element ): Iterator[Package] = new Iterator[Package] {
    private var p = ModelHelper.findParentOfType( e, classOf[Package] )

    def hasNext: Boolean = ( p != null )

    def next: Package = {
      val result = p
      p = p.getNestingPackage()
      result
    }
  }

  def getAllImportedPackages( p: Package ): Set[Package] = {

    def collectImportedPackages( imports: List[PackageImport], imported: Set[Package] ): Set[Package] =
      imports match {
        case Nil => imported
        case x :: xs =>
          val ip = x.getImportedPackage()
          if ( imported.contains( ip ) ) collectImportedPackages( xs, imported )
          else collectImportedPackages( ip.getPackageImport().toList ++ xs, imported + ip )
      }

    if ( p == null ) Set()
    else collectImportedPackages( p.getPackageImport().toList, Set() )
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
    val compatibleAccess = elements.forall { e => !mustBeEditable || e.isEditable() }
    compatibleAccess
  }
}