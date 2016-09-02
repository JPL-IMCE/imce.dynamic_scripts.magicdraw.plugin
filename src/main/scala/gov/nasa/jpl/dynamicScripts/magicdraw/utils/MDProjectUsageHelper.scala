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