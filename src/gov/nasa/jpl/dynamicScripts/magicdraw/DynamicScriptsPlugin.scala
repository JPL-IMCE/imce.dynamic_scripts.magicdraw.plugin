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
package gov.nasa.jpl.dynamicScripts.magicdraw

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import javax.swing.Icon
import javax.swing.ImageIcon

import com.nomagic.actions.AMConfigurator
import com.nomagic.actions.ActionsCategory
import com.nomagic.actions.ActionsManager
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager
import com.nomagic.magicdraw.actions.ConfiguratorWithPriority
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.core.options.EnvironmentOptions
import com.nomagic.magicdraw.plugins.Plugin
import com.nomagic.magicdraw.plugins.PluginDescriptor
import com.nomagic.magicdraw.plugins.ResourceDependentPlugin
import com.nomagic.magicdraw.properties.Property
import com.nomagic.magicdraw.properties.StringProperty
import com.nomagic.magicdraw.ui.ImageMap16
import com.nomagic.magicdraw.ui.MagicDrawProgressStatusRunner
import com.nomagic.magicdraw.ui.dialogs.specifications.SpecificationDialogManager
import com.nomagic.magicdraw.uml.ClassTypes
import com.nomagic.magicdraw.uml.DiagramType
import com.nomagic.ui.ResizableIcon
import com.nomagic.ui.SwingImageIcon
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector

import gov.nasa.jpl.dynamicScripts.DynamicScriptsRegistry
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ClassifiedInstanceDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicScriptInfo
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicScriptsForInstancesOfKind
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.MetaclassDesignation
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.QName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.SName
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.StereotypedMetaclassDesignation
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.DynamicScriptsMainMenuActionsCategory
import gov.nasa.jpl.dynamicScripts.magicdraw.designations._
import gov.nasa.jpl.dynamicScripts.magicdraw.actions.RefreshableActionsCategory
import gov.nasa.jpl.dynamicScripts.magicdraw.browser.DynamicScriptsBrowserConfigurator
import gov.nasa.jpl.dynamicScripts.magicdraw.commands.LoadDynamicScriptFilesCommand
import gov.nasa.jpl.dynamicScripts.magicdraw.diagram.DynamicScriptsDiagramConfigurator
import gov.nasa.jpl.dynamicScripts.magicdraw.options.DynamicScriptsConfigurationProperty
import gov.nasa.jpl.dynamicScripts.magicdraw.options.DynamicScriptsOptions
import gov.nasa.jpl.dynamicScripts.magicdraw.specificationDialog.SpecificationNodeConfiguratorForApplicableDynamicScripts
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML
import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.ComputedCharacterization
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDGUILogHelper
import com.nomagic.magicdraw.evaluation.EvaluationConfigurator

import scala.collection.immutable._
import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet
import scala.collection.TraversableOnce.MonadOps
import scala.collection.TraversableOnce.OnceCanBuildFrom
import scala.collection.TraversableOnce._
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.{AnyVal, Boolean, Int, Option, None, Some, StringContext, Unit}
import scala.Predef.{Map => _, Set => _, _}

@scala.deprecated("", "")
class ImageMapHelper(val dsPlugin: DynamicScriptsPlugin) extends AnyVal {

  def linkImage(): ResizableIcon =
    ImageMap16.LINK

}

object ImageMapHelper {

  implicit def toImageMapHelper(dsPlugin: DynamicScriptsPlugin): ImageMapHelper =
    new ImageMapHelper(dsPlugin)
}

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsPlugin
  extends Plugin
    with ResourceDependentPlugin
    with EnvironmentOptions.EnvironmentChangeListener {

  import ImageMapHelper._

  private var registry: DynamicScriptsRegistry = null
  def getDynamicScriptsRegistry: DynamicScriptsRegistry = registry

  override def updateByEnvironmentProperties( props: java.util.List[Property] ): Unit =
    for ( prop <- props; if DynamicScriptsOptions.DYNAMIC_SCRIPT_CONFIGURATION_FILES_ID == prop.getID ) {
      prop match {
        case p: StringProperty => 
          updateRegistryForConfigurationFiles(
            DynamicScriptsConfigurationProperty.getDynamicScriptConfigurationFiles( p ) )
        case _ =>
          ()
      }
    }

  def updateRegistryForConfigurationFiles( files: List[String] ): Option[String] =
    DynamicScriptsRegistry.mergeDynamicScripts( DynamicScriptsRegistry.init(), files ) match {
      case ( r: DynamicScriptsRegistry, errors: List[String] ) =>
        registry = r
        import MDGUILogHelper._
        val guiLog = getGUILog
        val log = guiLog.getMDPluginsLog

        log.debug(
          s"${this.getPluginName()} -- updateRegistryForConfigurationFiles: "+
          s"current dynamic scripts registry:\n$registry" )

        ClassLoaderHelper.clearUnavailableDynamicScriptsExplanationCache()

        if ( errors.isEmpty )
          None
        else {
          val message = errors.mkString(
            s"There are ${errors.size} errors found when parsing DynamicScripts configuration files\n",
            "\n",
            "\n=> Check the DynamicScripts configuration files." )
          log.error( message )
          Application.getInstance.getGUILog.showError( message )
          Some( message )
        }
    }

  def getRelevantMetaclassComputedCharacterizations
  ( metaclassShortName: String )
  : Map[String, SortedSet[ComputedCharacterization]] = {
    val scripts = for {
      ( key, availableCS ) <- registry.metaclassCharacterizations
      applicableCS = availableCS.filter { cs: ComputedCharacterization =>
        cs.characterizesInstancesOf match {
          case MetaclassDesignation( SName( mName ) ) => 
            MagicDrawElementKindDesignation.getSuperclassesOfMetaclassShortName(metaclassShortName).contains(mName)
          case _                                      => 
            false
        }
      }
      if applicableCS.nonEmpty
    } yield key -> applicableCS
    scripts.toMap
  }

  def getRelevantClassifierComputedCharacterizations
  ( e: Element )
  : Map[String, SortedSet[ComputedCharacterization]] = {
    val meta = e.getClassType.asSubclass( classOf[Element] )
    val metaName = ClassTypes.getShortName( meta )
    MagicDrawElementKindDesignation.METACLASS_2_CLASSIFIER_QUERY.get( meta ) match {
      case None =>
        Map()
      case Some( query ) =>
        val scripts = for {
          c <- query( e )
          cls <- MDUML.getAllGeneralClassifiersIncludingSelf( c )
          clsQName = cls.getQualifiedName
          ( key, availableCS ) <- registry.classifierCharacterizations
          applicableCS = availableCS.filter { cs: ComputedCharacterization =>
            cs.characterizesInstancesOf match {
              case ClassifiedInstanceDesignation( SName( mName ), QName( qName ) ) =>
                MagicDrawElementKindDesignation.getSuperclassesOfMetaclassShortName(metaName).contains(mName) &&
                wildCardMatch( clsQName, qName )
              case _ =>
                false
            }
          }
          if applicableCS.nonEmpty
        } yield key -> applicableCS
        scripts.toMap
    }
  }

  def getRelevantStereotypeComputedCharacterizations
  ( metaclassName: String, profileQName: String, stereotypeQName: String )
  : Map[String, SortedSet[ComputedCharacterization]] = {
    val scripts = for {
      ( key, availableCS ) <- registry.stereotypedMetaclassCharacterizations
      applicableCS = availableCS.filter { cs: ComputedCharacterization =>
        cs.characterizesInstancesOf match {
          case StereotypedMetaclassDesignation( SName( mName ), QName( pfName ), QName( qName ) ) =>
            MagicDrawElementKindDesignation.getSuperclassesOfMetaclassShortName(metaclassName).contains(mName) && 
            wildCardMatch( profileQName, pfName ) && wildCardMatch( stereotypeQName, qName )
          case _ =>
            false
        }
      }
      if applicableCS.nonEmpty
    } yield key -> applicableCS
    scripts.toMap
  }

  def getRelevantStereotypedClassifierComputedCharacterizations
  ( e: Element )
  : Map[String, SortedSet[ComputedCharacterization]] = e match {
    case is: InstanceSpecification =>
      val scripts = for {
        c <- is.getClassifier
        cls <- MDUML.getAllGeneralClassifiersIncludingSelf( c )
        clsQName = cls.getQualifiedName
        ( key, availableCS ) <- registry.stereotypedClassifierCharacterizations
        applicableCS = availableCS.filter { cs: ComputedCharacterization =>
          cs.characterizesInstancesOf match {
            case ClassifiedInstanceDesignation( SName( mName ), QName( qName ) ) =>
              mName == "InstanceSpecification" && wildCardMatch( clsQName, qName )
            case _ =>
              false
          }
        }
        if applicableCS.nonEmpty
      } yield key -> applicableCS
      scripts.toMap
    case is: Connector =>
      val scripts = for {
        cls <- MDUML.getAllGeneralClassifiersIncludingSelf( is.getType )
        clsQName = cls.getQualifiedName

        ( key, availableCS ) <- registry.stereotypedClassifierCharacterizations
        applicableCS = availableCS.filter { cs: ComputedCharacterization =>
          cs.characterizesInstancesOf match {
            case ClassifiedInstanceDesignation( SName( mName ), QName( qName ) ) =>
              mName == "Connector" && wildCardMatch( clsQName, qName )
            case _ =>
              false
          }
        }
        if applicableCS.nonEmpty
      } yield key -> applicableCS
      scripts.toMap
    case _ =>
      Map()
  }

  def getRelevantMetaclassActions
  ( metaclassShortName: String, criteria: DynamicActionScript => Boolean )
  : Map[String, Seq[DynamicActionScript]] = {
    val scripts = for {
      ( key, scripts ) <- registry.metaclassActions
      sScript <- scripts.filter { s: DynamicScriptsForInstancesOfKind =>
        s.applicableTo match {
          case MetaclassDesignation( SName( mName ) ) => 
            MagicDrawElementKindDesignation.getSuperclassesOfMetaclassShortName(metaclassShortName).contains(mName)
          case _                                      => 
            false
        }
      }
      availableActions = sScript.scripts filter criteria
      if availableActions.nonEmpty
    } yield key -> availableActions
    
    scripts.toMap
  }

  def getRelevantStereotypeActions
  ( metaclassName: String, profileQName: String, stereotypeQName: String, criteria: DynamicActionScript => Boolean )
  : Map[String, Seq[DynamicActionScript]] = {
    val scripts = for {
      ( key, scripts ) <- registry.stereotypedMetaclassActions
      sScript <- scripts.filter { s: DynamicScriptsForInstancesOfKind =>
        s.applicableTo match {
          case StereotypedMetaclassDesignation( SName( mName ), QName( pfName ), QName( qName ) ) =>
            MagicDrawElementKindDesignation.getSuperclassesOfMetaclassShortName(metaclassName).contains(mName) && 
            wildCardMatch( profileQName, pfName ) && wildCardMatch( stereotypeQName, qName )
          case _ =>
            false
        }
      }
      availableActions = sScript.scripts filter criteria
      if availableActions.nonEmpty
    } yield key -> availableActions
    scripts.toMap
  }

  def getRelevantDiagramClassifierActions
  ( d: Diagram, criteria: DynamicActionScript => Boolean )
  : Map[String, Seq[DynamicActionScript]] =
    ( for {
      ( key, scripts ) <- registry.classifierActions
      cScript <- scripts.filter { s: DynamicScriptsForInstancesOfKind =>
        MagicDrawElementKindDesignation.getMagicDrawDesignation( Project.getProject( d ), s.applicableTo ) match {
          case _: UnresolvedMagicDrawDesignation =>
            false
          case d: ResolvedMagicDrawDesignation   =>
            d.isResolved
        }
      }
      availableActions = cScript.scripts filter criteria
      if availableActions.nonEmpty
    } yield key -> availableActions
    ) toMap

  def getRelevantClassifierActions
  ( e: Element, criteria: DynamicActionScript => Boolean )
  : Map[String, Seq[DynamicActionScript]] = {
    val meta = e.getClassType.asSubclass( classOf[Element] )
    val metaName = ClassTypes.getShortName( meta )
    MagicDrawElementKindDesignation.METACLASS_2_CLASSIFIER_QUERY.get( meta ) match {
      case None => Map()
      case Some( query ) =>
        val scripts = for {
          c <- query( e )
          cls <- MDUML.getAllGeneralClassifiersIncludingSelf( c )
          clsQName = cls.getQualifiedName
          ( key, scripts ) <- registry.classifierActions
          cScript <- scripts.filter { s: DynamicScriptsForInstancesOfKind =>
            s.applicableTo match {
              case ClassifiedInstanceDesignation( SName( mName ), QName( qName ) ) =>
                MagicDrawElementKindDesignation.getSuperclassesOfMetaclassShortName(metaName).contains(mName) &&
                wildCardMatch( clsQName, qName )
              case _ =>
                false
            }
          }
          availableActions = cScript.scripts filter criteria
          if availableActions.nonEmpty
        } yield key -> availableActions
        scripts.toMap
    }
  }

  def getRelevantStereotypedClassifierActions
  ( e: Element, criteria: DynamicActionScript => Boolean )
  : Map[String, Seq[DynamicActionScript]] = e match {
    case is: InstanceSpecification =>
      val scripts = for {
        c <- is.getClassifier
        cls <- MDUML.getAllGeneralClassifiersIncludingSelf( c )
        clsQName = cls.getQualifiedName
        ( key, scripts ) <- registry.stereotypedClassifierActions
        cScript <- scripts.filter { s: DynamicScriptsForInstancesOfKind =>
          s.applicableTo match {
            case ClassifiedInstanceDesignation( SName( mName ), QName( qName ) ) =>
              mName == "InstanceSpecification" && wildCardMatch( clsQName, qName )
            case _ =>
              false
          }
        }
        availableActions = cScript.scripts filter criteria
        if availableActions.nonEmpty
      } yield key -> availableActions
      scripts.toMap
    case is: Connector =>
      val scripts = for {
        cls <- MDUML.getAllGeneralClassifiersIncludingSelf( is.getType )
        clsQName = cls.getQualifiedName
        ( key, scripts ) <- registry.stereotypedClassifierActions
        cScript <- scripts.filter { s: DynamicScriptsForInstancesOfKind =>
          s.applicableTo match {
            case ClassifiedInstanceDesignation( SName( mName ), QName( qName ) ) =>
              mName == "Connector" && wildCardMatch( clsQName, qName )
            case _ =>
              false
          }
        }
        availableActions = cScript.scripts filter criteria
        if availableActions.nonEmpty
      } yield key -> availableActions
      scripts.toMap
    case _ =>
      Map()
  }

  private var projectListener: DynamicScriptsProjectListener = null
  private var options: DynamicScriptsOptions = null
  private var JPLsmall: ImageIcon = null
  private var JPLcustomLink: ResizableIcon = null

  def getCharacterizationContext: DynamicScriptsProjectListener = projectListener
  def getDynamicScriptsOptions: DynamicScriptsOptions = options

  def getJPLSmallIcon: ImageIcon = JPLsmall

  def getJPLCustomLinkIcon: ResizableIcon = JPLcustomLink

  def getDynamicScriptIcon( ds: DynamicScriptInfo ): Icon =
    ds.icon match {
      case None =>
        getJPLCustomLinkIcon
      case Some( iconPath ) =>
        try {
          val iconURL = new File( MDUML.getInstallRoot + File.separator + iconPath.path ).toURI.toURL
          new ImageIcon( iconURL )
        }
        catch {
          case e: MalformedURLException =>
            getJPLCustomLinkIcon
        }
    }

  override def getPluginName: String = getDescriptor.getName
  override def getPluginVersion: String = getDescriptor.getVersion
  override def isPluginRequired( p: Project ): Boolean = false

  val refreshableActionsCategories: Map[String, ActionsCategory with RefreshableActionsCategory] =
    Map( DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_ID -> DynamicScriptsMainMenuActionsCategory() )

  def getRefreshableActionsCategory( id: String ): ActionsCategory with RefreshableActionsCategory = {
    require( refreshableActionsCategories.contains( id ) )
    refreshableActionsCategories.get( id ).get
  }

  def doRefreshActionsCategories(): Unit =
    refreshableActionsCategories.values foreach ( _.doRefresh() )

  protected val loadDynamicScriptsRefreshCommand =
    new LoadDynamicScriptFilesCommand( () => { this.doRefreshActionsCategories() } )

  def loadDynamicScriptsFiles(): Unit =
    MagicDrawProgressStatusRunner
      .runWithProgressStatus( loadDynamicScriptsRefreshCommand, "Reload DynamicScripts", true, 0 )

  override def init(): Unit = {
    DynamicScriptsPlugin.instance = this

    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog

    log.info( s"INIT: >> ${this.getClass.getName}" )
    try {
      projectListener = new DynamicScriptsProjectListener()
      Application.getInstance.addProjectEventListener( projectListener )
      options = DynamicScriptsOptions.configureEnvironmentOptions()
      registry = DynamicScriptsRegistry.init()

      val pluginDescriptor: PluginDescriptor = this.getDescriptor
      val pluginDir: File = pluginDescriptor.getPluginDirectory
      val jplSmallIconFile: File =
        new File( pluginDir.getAbsolutePath + File.separator + "icons" + File.separator + "JPL.small.png" )
      if ( !( jplSmallIconFile.exists() && jplSmallIconFile.canRead ) )
        log.fatal( s"Cannot find icons/JPL.small.png in ${this.getPluginName}" )
      else
        try {
          val jplIconURL: URL = jplSmallIconFile.toURI.toURL
          JPLsmall = new ImageIcon( jplIconURL )
        }
        catch {
          case e: MalformedURLException =>
            log.fatal( s"Cannot find icons/JPL.small.png in ${this.getPluginName}", e )
        }
        finally {
        }

      val customLinkIconFile: File =
        new File( pluginDir.getAbsolutePath + File.separator + "icons" + File.separator + "CustomLinkIcon.png" )
      if ( !( customLinkIconFile.exists() && customLinkIconFile.canRead ) )
        log.fatal( "Cannot find CustomLinkAction.png as " + customLinkIconFile )
      else
        try {
          val jplIconURL: URL = customLinkIconFile.toURI.toURL
          JPLcustomLink = new SwingImageIcon( jplIconURL )
        }
        catch {
          case e: MalformedURLException =>
            log.fatal( "Cannot read CustomLinkAction.png from " + customLinkIconFile, e )
        }
        finally {
          if ( JPLcustomLink == null ) {
            JPLcustomLink = this.linkImage()
          }
        }
      			
			EvaluationConfigurator.getInstance
        .registerBinaryImplementers(classOf[DynamicScriptsPlugin].getClassLoader)

      val manager: ActionsConfiguratorsManager = ActionsConfiguratorsManager.getInstance
      manager.addMainMenuConfigurator( new AMConfigurator() {
        override def getPriority: Int = ConfiguratorWithPriority.MEDIUM_PRIORITY

        override def configure( manager: ActionsManager ): Unit = {
          if ( null == manager.getActionFor( DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_ID ) )
            manager.addCategory(
              getRefreshableActionsCategory( DynamicScriptsMainMenuActionsCategory.DYNAMIC_SCRIPTS_MENU_ID ) )
        }
      } )

      val browserConfigurator = new DynamicScriptsBrowserConfigurator()
      manager.addContainmentBrowserContextConfigurator( browserConfigurator )
      manager.addContainmentBrowserShortcutsConfigurator( browserConfigurator )
      manager.addContainmentBrowserToolbarConfigurator( browserConfigurator )

      val diagramConfigurator = new DynamicScriptsDiagramConfigurator()
      for ( diagramType <- DiagramType.getCreatableDiagramTypes ) {
        manager.addDiagramContextConfigurator( diagramType, diagramConfigurator )
        manager.addDiagramContextToolbarConfigurator( diagramType, diagramConfigurator )
        manager.addDiagramShortcutsConfigurator( diagramType, diagramConfigurator )
      }

      SpecificationDialogManager.getManager.addConfigurator(
        classOf[Element],
        new SpecificationNodeConfiguratorForApplicableDynamicScripts() )
    }
    finally {
      log.info( s"INIT: << ${this.getClass.getName}" )
    }
  }

  override def close(): Boolean = {
    import MDGUILogHelper._
    val guiLog = getGUILog
    val log = guiLog.getMDPluginsLog

    log.info( s"CLOSE: >> ${this.getClass.getName}" )
    try {
      Application.getInstance.removeProjectEventListener( projectListener )
      projectListener = null
      DynamicScriptsPlugin.instance = null
      true
    }
    finally {
      log.info( s"CLOSE: << ${this.getClass.getName}" )
    }
  }

  override def isSupported: Boolean = true
}

object DynamicScriptsPlugin {

  private var instance: DynamicScriptsPlugin = null
  def getInstance(): DynamicScriptsPlugin = instance

  /**
   * Since the dynamic scripts are no longer in the model, we shouldn't have to use this anymore.
   *
   * @see gov.nasa.jpl.magicdraw.ontorefactoring.OntoRefactoringProjectListener.configureProjectOnceForElementAndAppliedStereotypes(Project, Element, PresentationElement)
   */
  def getDynamicScriptsRegistryAndCharacterizationContextFor
  ( e: Element, d: Diagram )
  : ( DynamicScriptsRegistry, Set[Package] ) = {
    val elementPackageContext =
      for ( p <- MDUML.elementPackageContainmentIterator( e ) )
      yield MDUML.getAllImportedPackages( p )
    val diagramPackageContext =
      for ( p <- MDUML.elementPackageContainmentIterator( d ) )
        yield MDUML.getAllImportedPackages( p )
    val diagramDependencyContext =
      for ( dep <- d.getClientDependency; sup <- dep.getSupplier )
        yield sup match {
          case p: Package =>
            MDUML.getAllImportedPackages(p)
          case _ =>
            Set[Package]()
        }

    ( getInstance().getDynamicScriptsRegistry,
      elementPackageContext.toIterable.to[Iterable].flatten.toSet ++
        diagramPackageContext.toIterable.to[Iterable].flatten.toSet ++
        diagramDependencyContext.toIterable.to[Iterable].flatten.toSet )
  }

}
