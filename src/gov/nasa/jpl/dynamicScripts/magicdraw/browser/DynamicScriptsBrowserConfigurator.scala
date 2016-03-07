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
package gov.nasa.jpl.dynamicScripts.magicdraw.browser

import java.awt.event.MouseEvent
import java.lang.{System}

import com.nomagic.actions.AMConfigurator
import com.nomagic.actions.ActionsManager
import com.nomagic.magicdraw.actions.ConfiguratorWithPriority
import com.nomagic.magicdraw.actions.MDActionsCategory
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.ui.browser.Node
import com.nomagic.magicdraw.ui.browser.Tree
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.magicdraw._
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BrowserContextMenuAction
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.DynamicBrowserContextMenuActionForTriggerAndSelection
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper
import gov.nasa.jpl.magicdraw.enhanced.ui.browser.EnhancedBrowserContextAMConfigurator

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.immutable._
import scala.{Boolean, Int, Option, None, Some, StringContext, Unit}
import scala.Predef.String
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsBrowserConfigurator
  extends EnhancedBrowserContextAMConfigurator
    with AMConfigurator {

  override def getPriority: Int = ConfiguratorWithPriority.HIGH_PRIORITY

  override def configure( manager: ActionsManager ): Unit = {}

  /**
   * This will be called by AspectJ advice...
    *
    * @see gov.nasa.jpl.magicdraw.advice.ui.browser.EnhanceBrowserContextConfiguratorWithShowPopupMenuAdvice
   */
  override def configure
  ( manager: ActionsManager,
    tree: Tree,
    mouseEvent: MouseEvent,
    trigger: Node,
    selection: java.util.Collection[Node] )
  : Unit = {

    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog
    val previousTime = System.currentTimeMillis()
      
    def getSelectedElement
    ( node: Node )
    : Option[Element] =
      node.getUserObject match {
        case e: Element => Some(e)
        case _ => None
      }

    getSelectedElement( trigger ) match {
      case None => ()
      case Some( e: Element ) =>
        Option.apply(
          manager.getCategory( DynamicScriptsBrowserConfigurator.DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID )
        ).fold[Unit]{
          val category = new MDActionsCategory(
            DynamicScriptsBrowserConfigurator.DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID,
            DynamicScriptsBrowserConfigurator.DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_NAME )
          category.setNested( true )
          manager.addCategory( 0, category )

          configure( category, tree, mouseEvent, trigger, e, selection.toList flatMap getSelectedElement )
          ()
        }{ category =>
          ()
        }
    }

    val currentTime = System.currentTimeMillis()
    log.info( s"DynamicScriptsBrowserConfigurator.configure took ${prettyDurationFromTo(previousTime, currentTime)}" )
  }

  def configure
  ( category: MDActionsCategory,
    tree: Tree,
    mouseEvent: MouseEvent,
    triggerNode: Node,
    triggerElement: Element,
    selected: List[Element] )
  : Unit = {

    val p = DynamicScriptsPlugin.getInstance()
    val mName = ClassTypes.getShortName( triggerElement.getClassType )
    
    def dynamicScriptMenuFilter( das: DynamicActionScript ): Boolean =
      DynamicScriptsBrowserConfigurator.isDynamicContextMenuScriptActionAvailable( das )

    val mActions = p.getRelevantMetaclassActions( mName, dynamicScriptMenuFilter )
    val sActions = StereotypesHelper.getStereotypesHierarchy( triggerElement ) flatMap { s =>
      Option
        .apply(s.getProfile)
        .fold[Map[String, Seq[DynamicActionScript]]](Map()) { pf =>
        p.getRelevantStereotypeActions(
          mName, pf.getQualifiedName, s.getQualifiedName, dynamicScriptMenuFilter
        )
      }
    }
    val cActions = p.getRelevantClassifierActions( triggerElement, dynamicScriptMenuFilter )

    val allDynamicScriptActions: Map[String, Seq[DynamicActionScript]] = mActions ++ sActions ++ cActions
    val project = Project.getProject( triggerElement )
    val configurator =
      DynamicScriptsBrowserConfigurator
      .createContextMenuActionForTriggerAndSelection( project, tree, triggerNode, triggerElement, selected )( _ )
    for {
      ( key, dynamicScriptActions ) <- allDynamicScriptActions
      menuActions = dynamicScriptActions flatMap { a => configurator( a ) }
      if menuActions.nonEmpty
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
object DynamicScriptsBrowserConfigurator {

  val DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID: String = "DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_ID"
  val DYNAMIC_SCRIPTS_BROWSER_CONTEXT_MENU_CATEGORY_NAME: String = "DynamicScriptsContextMenu"

  def isDynamicContextMenuScriptActionAvailable( das: DynamicActionScript ): Boolean = das match {
    case c: BrowserContextMenuAction =>
      ClassLoaderHelper.isDynamicActionScriptAvailable( c )
    case _                           =>
      false
  }

  def createContextMenuActionForTriggerAndSelection
  ( project: Project,
    tree: Tree,
    triggerNode: Node,
    triggerElement: Element,
    selected: List[Element] )
  ( dynamicActionScript: DynamicActionScript )
  : Option[DynamicBrowserContextMenuActionForTriggerAndSelection] =
    dynamicActionScript match {
      case s: BrowserContextMenuAction =>
        Some( DynamicBrowserContextMenuActionForTriggerAndSelection(
          project, tree, triggerNode, triggerElement, selected, s, null, null ) )
      case _                           =>
        None
    }
}
