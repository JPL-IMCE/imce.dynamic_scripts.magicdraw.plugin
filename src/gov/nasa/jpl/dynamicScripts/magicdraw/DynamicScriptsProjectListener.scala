/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2015, California Institute of Technology ("Caltech").
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