/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import scala.collection.JavaConversions.mapAsJavaMap
import scala.language.postfixOps

import com.nomagic.magicdraw.core.GUILog
import com.nomagic.magicdraw.uml.actions.SelectInContainmentTreeRunnable
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element

import org.apache.log4j.Logger

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
object MDGUILogHelper {

  def getMDPluginsLog(): Logger = MDLog.getPluginsLog()
  
  /**
   * The format string is expected to have one or more '<A>%s</A>' hyperlink format specifiers 
   * for a corresponding String component in the sequence of tuple links.
   * There should be as many hyperlink format specifiers as tuples in the links sequence.
   * The MD GUI Log will show the formatted, hyperlinked string obtained by replacing 
   * each Java format specifier with the corresponding String component of each tuple in the sequence of links.
   */
  def addGUILogHyperlink( guiLog: GUILog, format: String, links: (String, Element)*): Unit = {
    val linkedFormat = String.format(format, (links map { case ( (label:String, element: Element) ) => label }) : _*)  
    val linkMap = (links map { case ( (label:String, element: Element) ) => ( label -> new SelectInContainmentTreeRunnable( element ) ) }).toMap
    guiLog.addHyperlinkedText( linkedFormat, linkMap )
  }

}