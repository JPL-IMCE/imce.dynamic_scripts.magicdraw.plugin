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
package gov.nasa.jpl.dynamicScripts.magicdraw.diagram

import java.awt.Point
import java.lang.System

import scala.collection.immutable._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.{AnyVal, Array, Boolean, Int, Option, None, Some, StringContext, Unit}
import scala.Predef.{identity, refArrayOps, String}

import com.nomagic.actions.AMConfigurator
import com.nomagic.actions.ActionsCategory
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
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths.internal.DrawPathDiagramActionHelper._
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes.internal.DrawShapeDiagramActionHelper._
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.paths._
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes._
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

@scala.deprecated("", "")
class MDDiagramHelper(val d: DiagramPresentationElement) extends AnyVal {

  def hasTrigger: Option[PresentationElement] =
    d.getDiagramSurface match {
      case dc: DiagramCanvas =>
        dc.getLastPopupTriggerPoint match {
          case point: Point =>
            Option.apply(d.getPresentationElementAt(point))
          case _ =>
            Option.empty[PresentationElement]
        }
      case _ =>
        Option.empty[PresentationElement]
    }

}

object MDDiagramHelper {

  implicit def toMDDiagramHelper(d: DiagramPresentationElement): MDDiagramHelper =
  new MDDiagramHelper(d)

}

class DynamicScriptsDiagramConfigurator
  extends AMConfigurator
  with DiagramContextToolbarAMConfigurator
  with DiagramContextAMConfigurator {

  override def getPriority(): Int = ConfiguratorWithPriority.HIGH_PRIORITY
  override def configure( manager: ActionsManager ): Unit = {}

  override def configure( manager: ActionsManager, pElement: PresentationElement ): Unit = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog()
    val previousTime = System.currentTimeMillis()
    Option.apply(pElement)
    .fold[Unit](()){ pe =>
      for {
        d <- Option.apply(pe.getDiagramPresentationElement)
        e <- Option.apply(pe.getElement)
      } configureContextToolbar( manager, d, pe, e )
    }
    val currentTime = System.currentTimeMillis()
    log.info(
      "DynamicScriptsDiagramConfigurator.configure(manager, pElement) took "+
      s"${currentTime - previousTime} ms" )
  }

  def getOrCreateActionsManagerCategory
  ( manager: ActionsManager, categoryName: String )
  : ActionsCategory =
    manager.getCategories.find { ac => ac.getName == categoryName }
    .fold[ActionsCategory]({
      val category = new MDActionsCategory(categoryName, categoryName)
      category.setDisplayHeader(true)
      category.setNested(false)
      manager.addCategory(0, category)
      category
    }) { ac =>
      ac
    }

  protected def configureContextToolbar
  ( manager: ActionsManager,
    diagram: DiagramPresentationElement,
    pElement: PresentationElement,
    element: Element )
  : Unit = {
    val d = diagram.getDiagram
    val ds = StereotypesHelper.getStereotypes( d ).toList

    val p = DynamicScriptsPlugin.getInstance()
    val mName = ClassTypes.getShortName( element.getClassType )

    def dynamicScriptToolbarFilter( das: DynamicActionScript ): Boolean =
      DynamicScriptsDiagramConfigurator
        .isDynamicPathToolbarScriptActionAvailable( d, diagram.getDiagramType.getType, ds )( das )

    val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptToolbarFilter )
    val sActions = StereotypesHelper.getStereotypesHierarchy( element ) flatMap ( s =>
      Option.apply( s.getProfile ) match {
        case Some( pf ) =>
          p.getRelevantStereotypeActions(
            mName, pf.getQualifiedName, s.getQualifiedName, dynamicScriptToolbarFilter )
        case None =>
          Map[String, Seq[DynamicActionScript]]()
      } )
    val cActions = p.getRelevantClassifierActions( element, dynamicScriptToolbarFilter )
    val csActions = p.getRelevantStereotypedClassifierActions( element, dynamicScriptToolbarFilter )

    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] =
      mActions ++ sActions ++ cActions ++ csActions

    val project = Project.getProject( element )
    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      toolbarActions = dynamicScriptActions.flatMap(
        DynamicScriptsDiagramConfigurator
        .createDrawPathContextToolbarAction( project, diagram, pElement, element )
      )
      if toolbarActions.nonEmpty
    } {
      val category = getOrCreateActionsManagerCategory( manager, key )
      category.addActions( toolbarActions )
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
  override def configure
  ( manager: ActionsManager,
    diagram: DiagramPresentationElement,
    selected: Array[PresentationElement],
    requestor: PresentationElement )
  : Unit = {

    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog
    val previousTime = System.currentTimeMillis()

    val hasTrigger: Option[PresentationElement] = {
      import MDDiagramHelper._
      diagram.hasTrigger
    }

    Option.apply(
      manager.getCategory(
        DynamicScriptsDiagramConfigurator.DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID )
    ).fold[Unit]({
      val category = new MDActionsCategory(
        DynamicScriptsDiagramConfigurator.DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID,
        DynamicScriptsDiagramConfigurator.DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_NAME )
      category.setNested( true )
      manager.addCategory( 0, category )

      hasTrigger.fold[Unit]({
        if ( selected.nonEmpty )
        // no trigger & some selection
          configureDiagramContextMenuToolbarForMultipleSelection( category, diagram, selected.toList )
        else
        // no trigger & no selection
          configureDiagramContextMenuToolbarForNoTriggerAndNoSelection( category, diagram )
      }) { trigger =>
        // trigger & selection
        configureDiagramContextMenuToolbarForTriggerAndSelection(category, diagram, trigger, selected.toList)
      }
    }){ _ => () }

    val currentTime = System.currentTimeMillis()
    log.info(
      "DynamicScriptsDiagramConfigurator.configure(manager, diagram, selected, requestor) took " +
      s"${currentTime - previousTime} ms" )
  }

  def getOrCreateActionsSubCategory
  ( category: ActionsCategory, categoryName: String )
  : ActionsCategory =
    category.getCategories.find { ac =>
      ac.getName == categoryName
    }
    .fold[ActionsCategory]({
      val subCategory = new MDActionsCategory(categoryName, categoryName)
      subCategory.setEnabled(true)
      subCategory.setNested(true)
      category.addAction(subCategory)
      subCategory
    }){ ac =>
      ac
    }

  protected def configureDiagramContextMenuToolbarForTriggerAndSelection
  ( category: MDActionsCategory,
    diagram: DiagramPresentationElement,
    trigger: PresentationElement,
    selected: List[PresentationElement] )
  : Unit =
    Option.apply(trigger.getElement)
    .fold[Unit](()) {
      case element: Element =>

        val d = diagram.getDiagram
        val ds = StereotypesHelper.getStereotypes( d ).toList

        val p = DynamicScriptsPlugin.getInstance()
        val mName = ClassTypes.getShortName( element.getClassType )

        def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean =
          DynamicScriptsDiagramConfigurator
            .isDynamicContextMenuScriptActionAvailable( d, diagram.getDiagramType.getType, ds )( das )

        val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter )
        val sActions = StereotypesHelper.getStereotypesHierarchy( element ) flatMap ( s =>
          Option.apply( s.getProfile ) match {
            case Some( pf ) =>
              p.getRelevantStereotypeActions(
                mName, pf.getQualifiedName, s.getQualifiedName, dynamicScriptMenuFilter )
            case None =>
              Map[String, Seq[DynamicActionScript]]()
          } )
        val cActions = p.getRelevantClassifierActions( element, dynamicScriptMenuFilter )

        val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = mActions ++ sActions ++ cActions
        val project = Project.getProject( element )
        for {
          ( key, dynamicScriptActions ) <- allDynamicScriptActions
          menuActions = dynamicScriptActions.flatMap(
            DynamicScriptsDiagramConfigurator
            .createContextMenuActionForTriggerAndSelection( project, diagram, trigger, element, selected )
          )
          if menuActions.nonEmpty
        } {
          val subCategory = getOrCreateActionsSubCategory( category, key )
          subCategory.addActions( menuActions )
        }
    }

  protected def configureDiagramContextMenuToolbarForMultipleSelection(
    category: MDActionsCategory,
    diagram: DiagramPresentationElement,
    selected: List[PresentationElement] ): Unit = {
    val d = diagram.getDiagram
    val ds = StereotypesHelper.getStereotypes( d ).toList

    val p = DynamicScriptsPlugin.getInstance()
    val elements = selected.flatMap { pe => Option.apply(pe.getElement) }
    val projects = elements.flatMap { e => Option.apply(Project.getProject( e )) }.toSet
    if ( projects.size != 1 )
      return
    val project = projects.head

    def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean =
      DynamicScriptsDiagramConfigurator
      .isDynamicContextMenuScriptActionAvailable( d, diagram.getDiagramType.getType, ds )( das )

    val mActions =
      elements
      .map ( e => ClassTypes.getShortName( e.getClassType ) )
      .flatMap ( mName => p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter ) )

    val sActions = elements.flatMap { e =>
      val mName = ClassTypes.getShortName( e.getClassType )
      StereotypesHelper.getStereotypesHierarchy( e ) flatMap ( s =>
        Option.apply( s.getProfile ) match {
          case Some( pf ) =>
            p.getRelevantStereotypeActions(
              mName, pf.getQualifiedName, s.getQualifiedName, dynamicScriptMenuFilter )
          case None =>
            Map[String, Seq[DynamicActionScript]]()
        } )
    }

    val cActions =
      elements
      .flatMap ( e => p.getRelevantClassifierActions( e, dynamicScriptMenuFilter ) )

    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] =
      ( mActions ++ sActions ++ cActions ).toMap

    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      menuActions = dynamicScriptActions.flatMap(
        DynamicScriptsDiagramConfigurator
        .createContextMenuActionForMultipleSelection( project, diagram, selected )
      )
      if menuActions.nonEmpty
    } {
      val subCategory = getOrCreateActionsSubCategory( category, key )
      subCategory.addActions( menuActions )
    }
  }

  protected def configureDiagramContextMenuToolbarForNoTriggerAndNoSelection(
    category: MDActionsCategory,
    diagram: DiagramPresentationElement ): Unit = {
    val d = diagram.getDiagram
    val ds = StereotypesHelper.getStereotypes( d ).toList

    val p = DynamicScriptsPlugin.getInstance()
    val mName = ClassTypes.getShortName( d.getClassType )

    def dynamicShapeMenuFilter( das: DynamicActionScript ): Boolean =
      DynamicScriptsDiagramConfigurator
        .isDynamicShapeToolbarScriptActionAvailable( d, diagram.getDiagramType.getType, ds )( das )

    val mShapes = p.getRelevantMetaclassActions( mName, dynamicShapeMenuFilter )
    val sShapes = StereotypesHelper.getStereotypesHierarchy( d ).flatMap ( s =>
      Option.apply( s.getProfile ) match {
        case Some( pf ) =>
          p.getRelevantStereotypeActions(
            mName, pf.getQualifiedName, s.getQualifiedName, dynamicShapeMenuFilter )
        case None =>
          Map[String, Seq[DynamicActionScript]]()
      } )
    val cShapes = p.getRelevantDiagramClassifierActions( d, dynamicShapeMenuFilter )
    val allDynamicShapeActions: Map[String, Seq[DynamicActionScript]] = mShapes ++ sShapes ++ cShapes

    def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean =
      DynamicScriptsDiagramConfigurator.isDynamicContextMenuScriptActionAvailable(
        d, diagram.getDiagramType.getType, ds )( das )

    val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter )
    val sActions = StereotypesHelper.getStereotypesHierarchy( d ) flatMap ( s =>
      Option.apply( s.getProfile ) match {
        case Some( pf ) =>
          p.getRelevantStereotypeActions(
            mName, pf.getQualifiedName, s.getQualifiedName, dynamicScriptMenuFilter )
        case None =>
          Map[String, Seq[DynamicActionScript]]()
      } )
    val cActions = p.getRelevantClassifierActions( d, dynamicScriptMenuFilter )
    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = mActions ++ sActions ++ cActions

    val project = Project.getProject( d )
    for {
      ( key, dynamicShapeActions ) <- allDynamicShapeActions
      menuActions = dynamicShapeActions.flatMap(
        DynamicScriptsDiagramConfigurator
        .createDrawShapeContextToolbarAction( project, diagram )
      )
      if menuActions.nonEmpty
    } {
      val subCategory = new MDActionsCategory( key, key )
      subCategory.setEnabled( true )
      subCategory.setNested( true )
      subCategory.setActions( menuActions )
      category.addAction( subCategory )
    }

    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      menuActions = dynamicScriptActions.flatMap(
        DynamicScriptsDiagramConfigurator
        .createContextMenuActionForNoTriggerAndNoSelection( project, diagram )
      )
      if menuActions.nonEmpty
    } {
      val subCategory = getOrCreateActionsSubCategory( category, key )
      subCategory.addActions( menuActions )
    }
  }
}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object DynamicScriptsDiagramConfigurator {

  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_TOOLBAR_CATEGORY_ID: String =
    "DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_TOOLBAR_CATEGORY_ID"
  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_TOOLBAR_CATEGORY_NAME: String =
    "DynamicScriptsContextToolbar"

  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID: String =
    "DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_ID"
  val DYNAMIC_SCRIPTS_DIAGRAM_CONTEXT_MENU_CATEGORY_NAME: String =
    "DynamicScriptsContextMenu"

  def createDrawShapeContextToolbarAction
  ( project: Project,
    diagram: DiagramPresentationElement )
  ( dynamicActionScript: DynamicActionScript )
  : Option[DynamicShapeDiagramContextToolbarAction] = dynamicActionScript match {
      case s: ToplevelShapeInstanceCreator =>
        val shapeCreator = s.elementShape match {
          case d: MetaclassDesignation                     =>
            DynamicShapeCreatorForMetaclassDesignation( project, d )
          case d: StereotypedMetaclassDesignation          =>
            DynamicShapeCreatorForStereotypedMetaclassDesignation( project, d )
          case d: ClassifiedInstanceDesignation            =>
            DynamicShapeCreatorForClassifiedInstanceDesignation( project, d )
          case d: StereotypedClassifiedInstanceDesignation =>
            DynamicShapeCreatorForStereotypedClassifiedInstanceDesignation( project, d )
        }
        val finalization = DynamicShapeFinalizationAction( s, shapeCreator )
        Some( DynamicShapeDiagramContextToolbarAction( diagram, finalization ) )
      case _ =>
        None
    }

  def createDrawPathContextToolbarAction
  ( project: Project,
    diagram: DiagramPresentationElement,
    pElement: PresentationElement,
    element: Element )
  ( dynamicActionScript: DynamicActionScript )
  : Option[DynamicPathDiagramContextToolbarAction] =
    dynamicActionScript match {
      case p: ToplevelPathInstanceCreator =>
        val pathCreator = p.elementPath match {
          case d: MetaclassDesignation                     =>
            DynamicPathCreatorForMetaclassDesignation( project, d )
          case d: StereotypedMetaclassDesignation          =>
            DynamicPathCreatorForStereotypedMetaclassDesignation( project, d )
          case d: ClassifiedInstanceDesignation            =>
            DynamicPathCreatorForClassifiedInstanceDesignation( project, d )
          case d: StereotypedClassifiedInstanceDesignation =>
            DynamicPathCreatorForStereotypedClassifiedInstanceDesignation( project, d )
        }
        val finalization = DynamicPathFinalizationAction( p, pathCreator )
        Some( DynamicPathDiagramContextToolbarAction( diagram, pElement, finalization ) )
      case _ =>
        None
    }

  def isDynamicPathToolbarScriptActionAvailable(
    d: Diagram,
    dType: String,
    ds: List[Stereotype] )( das: DynamicActionScript ): Boolean = das match {
    case c: DynamicContextPathCreationActionScript =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) &&
        MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def isDynamicShapeToolbarScriptActionAvailable(
    d: Diagram,
    dType: String,
    ds: List[Stereotype] )( das: DynamicActionScript ): Boolean = das match {
    case c: DynamicContextShapeCreationActionScript =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) &&
        MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def isDynamicToolbarScriptActionAvailable(
    das: DynamicActionScript,
    d: Diagram,
    dType: String,
    ds: List[Stereotype] ): Boolean = das match {
    case c: DynamicContextMenuActionScript =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) &&
        MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def isDynamicContextMenuScriptActionAvailable(
    d: Diagram,
    dType: String,
    ds: List[Stereotype] )( das: DynamicActionScript ): Boolean = das match {
    case c: DiagramContextMenuAction =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c ) &&
        MagicDrawElementKindDesignation.isDynamicContextDiagramActionScriptAvailable( c, d, dType, ds )
    case _ =>
      false
  }

  def createContextMenuActionForTriggerAndSelection
  ( project: Project,
    diagram: DiagramPresentationElement,
    trigger: PresentationElement,
    element: Element,
    selected: List[PresentationElement] )
  ( dynamicActionScript: DynamicActionScript )
  : Option[DynamicDiagramContextMenuActionForTriggerAndSelection] =
    dynamicActionScript match {
      case s: DiagramContextMenuAction =>
        Some(
          DynamicDiagramContextMenuActionForTriggerAndSelection(
            project, diagram, trigger, element, selected, s, null, null ) )
      case _ =>
        None
    }

  def createContextMenuActionForMultipleSelection
  ( project: Project,
    diagram: DiagramPresentationElement,
    selected: List[PresentationElement] )
  ( dynamicActionScript: DynamicActionScript )
  : Option[DynamicDiagramContextMenuActionForMultipleSelection] =
    dynamicActionScript match {
      case s: DiagramContextMenuAction =>
        Some(
          DynamicDiagramContextMenuActionForMultipleSelection(
            project, diagram, selected, s, null, null ) )
      case _ =>
        None
    }

  def createContextMenuActionForNoTriggerAndNoSelection
  ( project: Project,
    diagram: DiagramPresentationElement )
  ( dynamicActionScript: DynamicActionScript )
  : Option[DynamicDiagramContextMenuActionForDiagram] =
    dynamicActionScript match {
      case s: DiagramContextMenuAction =>
        Some(
          DynamicDiagramContextMenuActionForDiagram(
            project, diagram, s, null, null ) )
      case _ =>
        None
    }
}