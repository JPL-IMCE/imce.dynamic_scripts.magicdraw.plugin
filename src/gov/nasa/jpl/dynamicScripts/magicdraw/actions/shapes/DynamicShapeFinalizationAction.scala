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
package gov.nasa.jpl.dynamicScripts.magicdraw.actions.shapes

import java.awt.Point
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException

import scala.util.Failure
import scala.util.Success

import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.uml.symbols.PresentationElement
import com.nomagic.magicdraw.uml.symbols.manipulators.drawactions.AdditionalDrawAction
import com.nomagic.magicdraw.utils.MDLog

import gov.nasa.jpl.dynamicScripts.DynamicScriptsTypes.DynamicActionScript
import gov.nasa.jpl.dynamicScripts.magicdraw.ClassLoaderHelper

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class DynamicShapeFinalizationAction(
    val action: DynamicActionScript,
    val creatorHelper: DynamicShapeCreatorHelper) 
    extends AdditionalDrawAction {
	
	def getSortKey(): String = action.sortKey() 
	
	def isEnabled(): Boolean =
	  ClassLoaderHelper.isDynamicActionScriptAvailable(action) && creatorHelper.isResolved
	  
  override def execute(pe: PresentationElement, point: Point): Boolean = {
		val log = MDLog.getPluginsLog()
		val e = pe.getElement()
		
		val previousTime = System.currentTimeMillis()
	
		val message = action.prettyPrint("")
		val guiLog = Application.getInstance().getGUILog()
		val localClassLoader = Thread.currentThread().getContextClassLoader()
		try {		
		  val scriptCL = ClassLoaderHelper.createDynamicScriptClassLoader(action)
			Thread.currentThread().setContextClassLoader(scriptCL)

			val c = scriptCL.loadClass(action.className.jname)
			if (c == null) {
				val error = "${message}: class '${action.className.jname}' not found in project '${action.projectName.jname}'"
				log.error(error)
				guiLog.showError(error)
				return false;
			}

			val m = creatorHelper.lookupMethod(c, action) match {
			  case Failure(t) => 
				  val error = s"${message}: ${t.getMessage()}"
				  log.error(error, t)
				  guiLog.showError(error, t)
				  return false
			  case Success(m) => m
			}

			val r = creatorHelper.invokeMethod(m, action, pe, e)
			
			val currentTime = System.currentTimeMillis()
			log.info(s"${message} took ${currentTime - previousTime} ms")
	
			r match {
			  case b: java.lang.Boolean => b.booleanValue()
			  case _ => false
			}

		} catch {
		  case ex: InvocationTargetException =>
        val t = ex.getTargetException() match { case null => ex; case t => t }
        val ex_message = message + s"\n${t.getMessage()}"
    			log.error(ex_message, t)
			  guiLog.showError(ex_message, t)
			  return false
		  case ex @ (_:ClassNotFoundException | _:SecurityException | _:NoSuchMethodException | _:IllegalArgumentException  | _:IllegalAccessException  | _:MalformedURLException  | _:NoSuchMethodException) =>
        val ex_message = message + s"\n${ex.getMessage()}"
    			log.error(ex_message, ex)
			  guiLog.showError(ex_message, ex)
			  return false
		} finally {
			Thread.currentThread().setContextClassLoader(localClassLoader)
		}
	}

	override def afterExecute(pe: PresentationElement, point: Point): Unit = {}

}