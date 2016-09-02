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

package gov.nasa.jpl.dynamicScripts.magicdraw

import com.nomagic.magicdraw.core.Project
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter

import scala.Unit
/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
class DynamicScriptsProjectListener extends ProjectEventListenerAdapter {

  override def projectOpened( p: Project ): Unit = projectOpenedOrActivated( p )

  /**
   * BUG : Ask NoMagic if projectOpenedFromGUI(p) always follows projectOpened(p).
   *
   * That is, are there cases where we can have projectOpenedFromGUI(p) but not projectOpened(p)
   * projectOpenedOrActivated( p )
   */
  override def projectOpenedFromGUI( p: Project ): Unit = {}
  
  override def projectActivated( p: Project ): Unit = projectOpenedOrActivated( p )

  /**
   * BUG : Ask NoMagic if projectActivatedFromGUI(p) always follows projectActivated(p).
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