/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw

import scala.language.implicitConversions
import scala.language.postfixOps

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsProjectListener extends ProjectEventListenerAdapter {

  override def projectOpened( p: Project ): Unit = projectOpenedOrActivated( p )

  /**
   * @BUG : Ask NoMagic if projectOpenedFromGUI(p) always follows projectOpened(p).
   *
   * That is, are there cases where we can have projectOpenedFromGUI(p) but not projectOpened(p)
   * projectOpenedOrActivated( p )
   */
  override def projectOpenedFromGUI( p: Project ): Unit = {}
  
  override def projectActivated( p: Project ): Unit = projectOpenedOrActivated( p )

  /**
   * @BUG : Ask NoMagic if projectActivatedFromGUI(p) always follows projectActivated(p).
   *
   *  That is, are there cases where we can have projectActivatedFromGUI(p) but not projectActivated(p)
   *  projectOpenedOrActivated( p )
   */
  override def projectActivatedFromGUI( p: Project ): Unit = {}

  override def projectClosed( p: Project ): Unit = projectClosedOrDeactivated( p )

  override def projectDeActivated( p: Project ): Unit = projectClosedOrDeactivated( p )

  override def projectReplaced( oldP: Project, newP: Project ): Unit = {
    projectClosedOrDeactivated( oldP );
    projectOpenedOrActivated( newP )
  }

  protected def projectOpenedOrActivated( p: Project ): Unit = {
    DynamicScriptsPlugin.getInstance().loadDynamicScriptsFiles()
  }

  protected def projectClosedOrDeactivated( p: Project ): Unit = {
  }
}