/**
 * License Terms
 *
 * Copyright (c) 2014, California
 * Institute of Technology ("Caltech").  U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *
 *  *   Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *  *   Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the
 *      distribution.
 *
 *  *   Neither the name of Caltech nor its operating division, the Jet
 *      Propulsion Laboratory, nor the names of its contributors may be
 *      used to endorse or promote products derived from this software
 *      without specific prior written permission.
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
package gov.nasa.jpl.dynamicScripts.magicdraw.diagram

import java.awt.Point

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps

import com.nomagic.actions.AMConfigurator
import com.nomagic.actions.ActionsManager
import com.nomagic.magicdraw.actions.ConfiguratorWithPriority
import com.nomagic.magicdraw.actions.DiagramContextAMConfigurator
import com.nomagic.magicdraw.actions.DiagramContextToolbarAMConfigurator
import com.nomagic.magicdraw.actions.MDActionsCategory
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.DiagramCanvas
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DiagramContextMenuAction
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicContextMenuActionScript
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicContextPathCreationActionScript
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicContextShapeCreationActionScript
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelPathInstanceCreator
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ToplevelShapeInstanceCreator
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.DynamicDiagramContextMenuActionForDiagram
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.DynamicDiagramContextMenuActionForMultipleSelection
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.DynamicDiagramContextMenuActionForTriggerAndSelection
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.MagicDrawElementKindDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathCreatorForClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathCreatorForMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathCreatorForStereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathCreatorForStereotypedMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathCreatorHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathDiagramContextToolbarAction
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.DynamicPathFinalizationAction
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeCreatorForClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeCreatorForMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeCreatorForStereotypedClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeCreatorForStereotypedMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeCreatorHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeDiagramContextToolbarAction
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.DynamicShapeFinalizationAction
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsDiagramConfigurator extends AMConfigurator with DiagramContextToolbarAMConfigurator with DiagramContextAMConfigurator {

  override def getPriority(): Int = ConfiguratorWithPriority.HIGH_PRIORITY
  override def configure( manager: ActionsManager ): Unit = {}

  override def configure( manager: ActionsManager, pElement: PresentationElement ): Unit = pElement match {
    case null => ()
    case pe => ( pe.getDiagramPresentationElement(), pe.getElement() ) match {
      case ( null, _ ) => ()
      case ( _, null ) => ()
      case ( d, e )    => configureContextToolbar( manager, d, pe, e )
    }
  }

  def configureContextToolbar( manager: ActionsManager, diagram: DiagramPresentationElement, pElement: PresentationElement, element: Element ): Unit = {
    val log = MDLog.getPluginsLog()
    val d = diagram.getDiagram()
    val ds = StereotypesHelper.getStereotypes( d ).toList
    log.info(
      s"""|${this.getClass().getName()}.configureContextToolbar(diagram=${diagram.getDiagram().getQualifiedName()}
            |Element=${pElement.getClassType().getName()}
            |element=${ClassTypes.getShortName( element.getClassType() )}: ${element.getHumanName()}""".stripMargin )

    def dynamicScriptToolbarFilter( das: DynamicActionScript ): Boolean = DynamicScriptsDiagramConfigurator.isDynamicPathToolbarScriptActionAvailable( d, diagram.getDiagramType().getType(), ds )( das )

    val p = DynamicScriptsPlugin.getInstance()
    val mName = ClassTypes.getShortName( element.getClassType() )
    val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptToolbarFilter )
    val sActions = StereotypesHelper.getStereotypesHierarchy( element ) flatMap ( s => p.getRelevantStereotypeActions( mName, s.getProfile().getQualifiedName(), s.getQualifiedName(), dynamicScriptToolbarFilter ) )
    val cActions = p.getRelevantClassifierActions( element, dynamicScriptToolbarFilter )
    val csActions = p.getRelevantStereotypedClassifierActions( element, dynamicScriptToolbarFilter )

    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = mActions ++ sActions ++ cActions ++ csActions
    val project = Project.getProject( element )
    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      toolbarActions = dynamicScriptActions flatMap DynamicScriptsDiagramConfigurator.createDrawPathContextToolbarAction( project, diagram, pElement, element )
      if ( toolbarActions.nonEmpty )
    } {
      val category = new MDActionsCategory( key, key )
      category.setDisplayHeader( true )
      category.setNested( false )
      log.info( s"${this.getClass().getName()}.configureContextToolbar ${toolbarActions.size} actions ${( toolbarActions map { a => a.finalizationAction.action.name.hname } ).mkString( "\n => ", "\n => ", "\n" )}" )
      category.setActions( toolbarActions )
      manager.addCategory( 0, category )
    }
  }

  /**
   * @BUG
   *
   * MD Open API problem:
   * (selected, requestor) seems to be either:
   * 1: ([requestor], [requestor]) 	-- i.e., only 1 element is selected
   * 2: ([s1, s2, ...], null)				-- i.e., 2 or more elements are selected but the requestor is null
   * 3: ([], null)										-- i.e., no selection
   */

  /**
   * Workaround for cases 1 and 2:
   * determine the selected element that triggered MD to configure the diagram context menu
   */
  override def configure( manager: ActionsManager, diagram: DiagramPresentationElement, selected: Array[PresentationElement], requestor: PresentationElement ): Unit = {

    val trigger: PresentationElement = diagram.getDiagramSurface() match {
      case dc: DiagramCanvas => dc.getLastPopupTriggerPoint() match {
        case point: Point => diagram.getPresentationElementAt( point )
        case _            => null
      }
      case _ => null
    }

    if ( null == manager.getCategory( DynamicScriptsDiagramConfigurator.DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID ) ) {
      val category = new MDActionsCategory(
        DynamicScriptsDiagramConfigurator.DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID,
        DynamicScriptsDiagramConfigurator.DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_NAME )
      category.setNested( true )
      manager.addCategory( 0, category )

      if ( trigger != null )
        // trigger & selection
        configureDiagramContextMenuToolbarForTriggerAndSelection( category, diagram, trigger, selected.toList )
      else if ( selected.nonEmpty )
        // no trigger & some selection
        configureDiagramContextMenuToolbarForMultipleSelection( category, diagram, selected.toList )
      else
        // no trigger & no selection
        configureDiagramContextMenuToolbarForNoTriggerAndNoSelection( category, diagram )
    }
  }

  def configureDiagramContextMenuToolbarForTriggerAndSelection( category: MDActionsCategory, diagram: DiagramPresentationElement, trigger: PresentationElement, selected: List[PresentationElement] ): Unit = {
    trigger.getElement() match {
      case null => ()
      case element: Element =>

        val log = MDLog.getPluginsLog()
        val d = diagram.getDiagram()
        val ds = StereotypesHelper.getStereotypes( d ).toList

        log.info(
          s"""|${this.getClass().getName()}.configureDiagramContextMenuToolbarForTriggerAndSelection(category=${category.getName()},
          |trigger=${trigger.getClassType().getName()} : ${element.getHumanName}${( selected map { _.getElement().getHumanName() } ).mkString( "\nselected:\n =>", "\n => ", "\n" )}
          |diagram=${diagram.getDiagramType().getType()} : '${d.getQualifiedName()}' ${( ds map ( _.getQualifiedName() ) ) mkString ( "<<", ", ", ">>" )}""".stripMargin )

        def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean = DynamicScriptsDiagramConfigurator.isDynamicContextMenuScriptActionAvailable( d, diagram.getDiagramType().getType(), ds )( das )

        val p = DynamicScriptsPlugin.getInstance()
        val mName = ClassTypes.getShortName( element.getClassType() )
        val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter )
        val sActions = StereotypesHelper.getStereotypesHierarchy( element ) flatMap ( s => p.getRelevantStereotypeActions( mName, s.getProfile().getQualifiedName(), s.getQualifiedName(), dynamicScriptMenuFilter ) )
        val cActions = p.getRelevantClassifierActions( element, dynamicScriptMenuFilter )

        val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = mActions ++ sActions ++ cActions
        val project = Project.getProject( element )
        for {
          ( key, dynamicScriptActions ) <- allDynamicScriptActions
          menuActions = dynamicScriptActions flatMap DynamicScriptsDiagramConfigurator.createContextMenuActionForTriggerAndSelection( project, diagram, trigger, element, selected )
          if ( menuActions.nonEmpty )
        } {
          val subCategory = new MDActionsCategory( key, key )
          subCategory.setEnabled( true )
          subCategory.setNested( true )
          subCategory.setActions( menuActions )
          category.addAction( subCategory )
        }
    }
  }

  def configureDiagramContextMenuToolbarForMultipleSelection( category: MDActionsCategory, diagram: DiagramPresentationElement, selected: List[PresentationElement] ): Unit = {
    val log = MDLog.getPluginsLog()
    val d = diagram.getDiagram()
    val ds = StereotypesHelper.getStereotypes( d ).toList
    log.info(
      s"""|${this.getClass().getName()}.configureDiagramContextMenuToolbarForMultipleSelection(category=${category.getName()},
          |${( selected map { _.getElement().getHumanName() } ).mkString( "\nselected:\n =>", "\n => ", "\n" )},
          |diagram=${diagram.getDiagramType().getType()} : '${d.getQualifiedName()}' ${( ds map ( _.getQualifiedName() ) ) mkString ( "<<", ", ", ">>" )}""".stripMargin )

    def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean = DynamicScriptsDiagramConfigurator.isDynamicContextMenuScriptActionAvailable( d, diagram.getDiagramType().getType(), ds )( das )

    val p = DynamicScriptsPlugin.getInstance()
    val elements = selected map { pe => pe.getElement() }
    val projects = elements map { e => Project.getProject( e ) } toSet;
    if ( projects.size != 1 )
      return
    val project = projects.head

    val mActions = elements map ( e => ClassTypes.getShortName( e.getClassType() ) ) flatMap ( mName => p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter ) )
    val sActions = elements flatMap { e =>
      val mName = ClassTypes.getShortName( e.getClassType() )
      StereotypesHelper.getStereotypesHierarchy( e ) flatMap ( s => p.getRelevantStereotypeActions( mName, s.getProfile().getQualifiedName(), s.getQualifiedName(), dynamicScriptMenuFilter ) )
    }
    val cActions = elements flatMap ( e => p.getRelevantClassifierActions( e, dynamicScriptMenuFilter ) )

    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = ( mActions ++ sActions ++ cActions ) toMap;
    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      menuActions = dynamicScriptActions flatMap DynamicScriptsDiagramConfigurator.createContextMenuActionForMultipleSelection( project, diagram, selected )
      if ( menuActions.nonEmpty )
    } {
      val subCategory = new MDActionsCategory( key, key )
      subCategory.setEnabled( true )
      subCategory.setNested( true )
      subCategory.setActions( menuActions )
      category.addAction( subCategory )
    }
  }

  def configureDiagramContextMenuToolbarForNoTriggerAndNoSelection( category: MDActionsCategory, diagram: DiagramPresentationElement ): Unit = {
    val log = MDLog.getPluginsLog()
    val d = diagram.getDiagram()
    val ds = StereotypesHelper.getStereotypes( d ).toList
    log.info(
      s"""|${this.getClass().getName()}.configureDiagramContextMenuToolbarForNoTriggerAndNoSelection(category=${category.getName()},
          |diagram=${diagram.getDiagramType().getType()} : '${d.getQualifiedName()}' ${( ds map ( _.getQualifiedName() ) ) mkString ( "<<", ", ", ">>" )}""".stripMargin )

    val p = DynamicScriptsPlugin.getInstance()
    val mName = ClassTypes.getShortName( d.getClassType() )

    def dynamicShapeMenuFilter( das: DynamicActionScript ): Boolean = DynamicScriptsDiagramConfigurator.isDynamicShapeToolbarScriptActionAvailable( d, diagram.getDiagramType().getType(), ds )( das )

    val mShapes = p.getRelevantMetaclassActions( mName, dynamicShapeMenuFilter )
    val sShapes = StereotypesHelper.getStereotypesHierarchy( d ) flatMap ( s => p.getRelevantStereotypeActions( mName, s.getProfile().getQualifiedName(), s.getQualifiedName(), dynamicShapeMenuFilter ) )
    val cShapes = p.getRelevantDiagramClassifierActions( d, dynamicShapeMenuFilter )
    val allDynamicShapeActions: Map[String, Seq[DynamicActionScript]] = mShapes ++ sShapes ++ cShapes

    def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean = DynamicScriptsDiagramConfigurator.isDynamicContextMenuScriptActionAvailable( d, diagram.getDiagramType().getType(), ds )( das )

    val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter )
    val sActions = StereotypesHelper.getStereotypesHierarchy( d ) flatMap ( s => p.getRelevantStereotypeActions( mName, s.getProfile().getQualifiedName(), s.getQualifiedName(), dynamicScriptMenuFilter ) )
    val cActions = p.getRelevantClassifierActions( d, dynamicScriptMenuFilter )
    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = mActions ++ sActions ++ cActions

    val project = Project.getProject( d )
    for {
      ( key, dynamicShapeActions ) <- allDynamicShapeActions
      menuActions = dynamicShapeActions flatMap DynamicScriptsDiagramConfigurator.createDrawShapeContextToolbarAction( project, diagram )
      if ( menuActions.nonEmpty )
    } {
      val subCategory = new MDActionsCategory( key, key )
      subCategory.setEnabled( true )
      subCategory.setNested( true )
      subCategory.setActions( menuActions )
      category.addAction( subCategory )
    }

    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      menuActions = dynamicScriptActions flatMap DynamicScriptsDiagramConfigurator.createContextMenuActionForNoTriggerAndNoSelection( project, diagram )
      if ( menuActions.nonEmpty )
    } {
      val subCategory = new MDActionsCategory( key, key )
      subCategory.setEnabled( true )
      subCategory.setNested( true )
      subCategory.setActions( menuActions )
      category.addAction( subCategory )
    }
  }
}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsDiagramConfigurator {

  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_TOOLBAR_CATEGORY_ID: String = "DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_TOOLBAR_CATEGORY_ID"
  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_TOOLBAR_CATEGORY_NAME: String = "DynamicScriptsContextToolbar"

  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID: String = "DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID"
  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_NAME: String = "DynamicScriptsContextMenu"

  def createDrawShapeContextToolbarAction( project: Project, diagram: DiagramPresentationElement )( dynamicActionScript: DynamicActionScript ): Option[DynamicShapeDiagramContextToolbarAction] = {
    val creator: Option[DynamicShapeCreatorHelper] = dynamicActionScript match {
      case s: ToplevelShapeInstanceCreator =>
        s.elementShape match {
          case d: MetaclassDesignation                     => Some( DynamicShapeCreatorForMetaclassDesignation( project, d ) )
          case d: StereotypedMetaclassDesignation          => Some( DynamicShapeCreatorForStereotypedMetaclassDesignation( project, d ) )
          case d: ClassifiedInstanceDesignation            => Some( DynamicShapeCreatorForClassifiedInstanceDesignation( project, d ) )
          case d: StereotypedClassifiedInstanceDesignation => Some( DynamicShapeCreatorForStereotypedClassifiedInstanceDesignation( project, d ) )
        }
      case _ => None
    }
    creator match {
      case None      => None
      case Some( c ) => Some( DynamicShapeDiagramContextToolbarAction( diagram, DynamicShapeFinalizationAction( dynamicActionScript, c ) ) )
    }
  }

  def createDrawPathContextToolbarAction( project: Project, diagram: DiagramPresentationElement, pElement: PresentationElement, element: Element )( dynamicActionScript: DynamicActionScript ): Option[DynamicPathDiagramContextToolbarAction] = {
    val creator: Option[DynamicPathCreatorHelper] = dynamicActionScript match {
      case p: ToplevelPathInstanceCreator =>
        p.elementPath match {
          case d: MetaclassDesignation => Some( DynamicPathCreatorForMetaclassDesignation( project, d ) )
          case d: StereotypedMetaclassDesignation => Some( DynamicPathCreatorForStereotypedMetaclassDesignation( project, d ) )
          case d: ClassifiedInstanceDesignation => Some( DynamicPathCreatorForClassifiedInstanceDesignation( project, d ) )
          case d: StereotypedClassifiedInstanceDesignation => Some( DynamicPathCreatorForStereotypedClassifiedInstanceDesignation( project, d ) )
          case _ => None
        }
      case _ => None
    }
    creator match {
      case None =>
        None
      case Some( c ) =>
        Some( DynamicPathDiagramContextToolbarAction( diagram, pElement, DynamicPathFinalizationAction( dynamicActionScript, c ) ) )
    }
  }

  def isDynamicPathToolbarScriptActionAvailable( d: Diagram, dType: String, ds: List[Stereotype] )( das: DynamicActionScript ): Boolean = das match {
    case c: DynamicContextPathCreationActionScript =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) && MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def isDynamicShapeToolbarScriptActionAvailable( d: Diagram, dType: String, ds: List[Stereotype] )( das: DynamicActionScript ): Boolean = das match {
    case c: DynamicContextShapeCreationActionScript =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) && MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def isDynamicToolbarScriptActionAvailable( das: DynamicActionScript, d: Diagram, dType: String, ds: List[Stereotype] ): Boolean = das match {
    case c: DynamicContextMenuActionScript =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) && MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def isDynamicContextMenuScriptActionAvailable( d: Diagram, dType: String, ds: List[Stereotype] )( das: DynamicActionScript ): Boolean = das match {
    case c: DiagramContextMenuAction =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) && MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def createContextMenuActionForTriggerAndSelection( project: Project, diagram: DiagramPresentationElement, trigger: PresentationElement, element: Element, selected: List[PresentationElement] )( dynamicActionScript: DynamicActionScript ): Option[DynamicDiagramContextMenuActionForTriggerAndSelection] =
    dynamicActionScript match {
      case s: DiagramContextMenuAction =>
        Some( DynamicDiagramContextMenuActionForTriggerAndSelection( project, diagram, trigger, element, selected, s, null, null ) )
      case _ =>
        None
    }

  def createContextMenuActionForMultipleSelection( project: Project, diagram: DiagramPresentationElement, selected: List[PresentationElement] )( dynamicActionScript: DynamicActionScript ): Option[DynamicDiagramContextMenuActionForMultipleSelection] =
    dynamicActionScript match {
      case s: DiagramContextMenuAction =>
        Some( DynamicDiagramContextMenuActionForMultipleSelection( project, diagram, selected, s, null, null ) )
      case _ =>
        None
    }

  def createContextMenuActionForNoTriggerAndNoSelection( project: Project, diagram: DiagramPresentationElement )( dynamicActionScript: DynamicActionScript ): Option[DynamicDiagramContextMenuActionForDiagram] =
    dynamicActionScript match {
      case s: DiagramContextMenuAction =>
        Some( DynamicDiagramContextMenuActionForDiagram( project, diagram, s, null, null ) )
      case _ =>
        None
    }
}
