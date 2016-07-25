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
package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import java.io.File
import java.lang.{IllegalArgumentException, System}

import com.nomagic.ci.persistence.IProject
import com.nomagic.ci.persistence.decomposition.ProjectAttachmentConfiguration
import com.nomagic.magicdraw.core.ProjectUtilities
import com.nomagic.magicdraw.core.modules.ModulesService
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory

import scala.{None, Option, Some, StringContext, Unit}
import scala.util.{Failure, Success, Try}

object MDProjectUsageHelper {

  def attachMagicDrawLocalProject(p: IProject, localProjectFile: File)
  : Try[Unit]
  = {
    val d = ProjectDescriptorsFactory.createProjectDescriptor(localProjectFile.toURI)
    val uri: org.eclipse.emf.common.util.URI = ProjectUtilities.getEMFURI(d.getURI)
    val cfg = ProjectUtilities.createDefaultProjectAttachmentConfiguration(uri)
    attachMagicDrawProject(p, cfg)
  }

  def attachMagicDrawProject(p: IProject, cfg: ProjectAttachmentConfiguration)
  : Try[Unit]
  = Option.apply(ModulesService.attachModuleOnTask(p, cfg)) match {
    case None =>
      Failure(new IllegalArgumentException(s"Failed to attach local project: $cfg"))
    case Some(attached) =>
      System.out.println(s"Successfully attached local project: $attached")
      Success(())
  }

}