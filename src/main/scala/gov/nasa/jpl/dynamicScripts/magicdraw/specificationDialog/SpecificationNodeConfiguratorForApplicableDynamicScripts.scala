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

package gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog

import java.lang.{System, Throwable}

import scala.collection.immutable._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.immutable.SortedSet
import scala.{Option, None, Some, StringContext, Unit}
import scala.Predef.String

import com.nomagic.magicdraw.core.{Application, Project}
import com.nomagic.magicdraw.ui.dialogs.specifications.configurator.ISpecificationNodeConfigurator
import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.ConfigurableNodeFactory
import com.nomagic.magicdraw.ui.dialogs.specifications.tree.node.IConfigurableNode
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import gov.nasa.jpl.dynamicScripts.magicdraw._
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BinaryDerivationRefresh.DELAYED_COMPUTATION_UNTIL_INVOKED
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.BinaryDerivationRefresh.EAGER_COMPUTATION_AS_NEEDED
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedDerivedFeature
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsProjectListener
import gov.nasa.jpl.dynamicScripts.magicdraw.designations.MagicDrawElementKindDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables._
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class SpecificationNodeConfiguratorForApplicableDynamicScripts extends ISpecificationNodeConfigurator {

  override def configure( node: IConfigurableNode, e: Element ): Unit = {
    val guiLog = Application.getInstance.getGUILog
    import MDGUILogHelper._
    val log = guiLog.getMDPluginsLog

    val previousTime = System.currentTimeMillis()
    try {
      val characterizationContext: DynamicScriptsProjectListener =
        DynamicScriptsPlugin.getInstance().getCharacterizationContext
      log.info( s"${this.getClass.getName}: configure specification dialog for ${e.getHumanType}}" )

      val project = Project.getProject( e )
      if ( project == null )
        return

      val p = DynamicScriptsPlugin.getInstance()

      val mName = ClassTypes.getShortName( e.getClassType )
      val es = StereotypesHelper.getStereotypes( e ).toList

      val mDerivedFeatures = p.getRelevantMetaclassComputedCharacterizations( mName )
      val sDerivedFeatures = StereotypesHelper.getStereotypesHierarchy( e ) flatMap ( s =>
        Option.apply( s.getProfile ) match {
          case Some( pf ) =>
            p.getRelevantStereotypeComputedCharacterizations(
              mName, pf.getQualifiedName, s.getQualifiedName )
          case None =>
            Map[String, SortedSet[DynamicScriptsTypes.ComputedCharacterization]]()
        } )
      val cDerivedFeatures = p.getRelevantClassifierComputedCharacterizations( e )
      val csDerivedFeatures = p.getRelevantStereotypedClassifierComputedCharacterizations( e )

      val allDerivedFeatures = mDerivedFeatures ++ sDerivedFeatures ++ cDerivedFeatures ++ csDerivedFeatures
      addDerivedFeatures( project, node, allDerivedFeatures, e )

    } finally {
      val currentTime = System.currentTimeMillis()
      log.info(
        s"SpecificationNodeConfiguratorForApplicableDynamicScripts.configure took "+
        prettyDurationFromTo(previousTime, currentTime))
    }
  }

  def addDerivedFeatures(
    project: Project,
    node: IConfigurableNode,
    allDerivedFeatures: Map[String, SortedSet[DynamicScriptsTypes.ComputedCharacterization]],
    e: Element ): Unit = {

    type DerivedHierarchicalTable = DerivedPropertiesHierarchicalTableModel[AbstractDisposableTableModel]

    val entries = allDerivedFeatures.keys.toList.sorted
    entries foreach { entry =>
      val characterizations = allDerivedFeatures( entry )
      characterizations foreach {
        case cs if cs.computedDerivedFeatures nonEmpty =>

          val ek = MagicDrawElementKindDesignation.getMagicDrawDesignation( project, cs.characterizesInstancesOf )

          val early =
            cs.computedDerivedFeatures
            .filter ( EAGER_COMPUTATION_AS_NEEDED == _.refresh )
            .flatMap ( computedFeatureToInfo( cs, e, ek, _ ) )

          val delayed = cs.computedDerivedFeatures filter ( DELAYED_COMPUTATION_UNTIL_INVOKED == _.refresh ) flatMap
            ( computedFeatureToInfo( cs, e, ek, _ ) )

          val earlyNode =
            ConfigurableNodeFactory.createConfigurableNode( SpecificationComputedNode[DerivedHierarchicalTable](
            ID = s"$entry EARLY",
            label = s"${cs.name.hname} (derivations computed early)",
            e,
            table = new DerivedHierarchicalTable( e, early ) ) )

          node.insertNode( IConfigurableNode.DOCUMENTATION_HYPERLINKS, IConfigurableNode.Position.BEFORE, earlyNode )

          if ( delayed nonEmpty ) {

            val delayedNode = ConfigurableNodeFactory.createConfigurableNode(
              SpecificationComputedNode[DerivedHierarchicalTable](
                ID = s"$entry DELAYED",
                label = s"${cs.name.hname} (derivations computed on-demand)",
                e,
                table = new DerivedHierarchicalTable( e, delayed ) ) )

            earlyNode.addNode( delayedNode )

          }
      }
    }
  }

  def computedFeatureToInfo(
    cs: DynamicScriptsTypes.ComputedCharacterization,
    e: Element,
    ek: MagicDrawElementKindDesignation,
    computedDerivedFeature: ComputedDerivedFeature ): Option[AbstractDisposableTableModel] =
    try {
      Some( computedDerivedFeature match {
        case f: DynamicScriptsTypes.ComputedDerivedProperty =>
          DerivedPropertyComputedRowInfo( cs, e, ek, f )
        case f: DynamicScriptsTypes.ComputedDerivedTable =>
          DerivedPropertyComputedTableInfo( cs, e, ek, f )
        case f: DynamicScriptsTypes.ComputedDerivedTree =>
          DerivedPropertyComputedTreeInfo( cs, e, ek, f )
        case f: DynamicScriptsTypes.ComputedDerivedWidget =>
          DerivedPropertyComputedWidget( cs, e, ek, f )
      } )
    } catch {
      case t: Throwable =>
        import MDGUILogHelper._
        val guiLog = getGUILog
        val log = guiLog.getMDPluginsLog
        log.error(s"Cannot create computed derived feature for '$computedDerivedFeature' because of an error", t)
        None
    }

}