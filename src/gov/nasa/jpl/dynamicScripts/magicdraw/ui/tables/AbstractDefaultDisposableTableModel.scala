/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.ui.tables

import java.awt.Component
import java.awt.Graphics
import java.awt.Rectangle

import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel

import com.jidesoft.grid.CellPainter
import com.jidesoft.grid.CellStyle
import com.jidesoft.grid.StyleModel
import com.jidesoft.swing.OverlayableIconsFactory

import gov.nasa.jpl.dynamicScripts.magicdraw.ui.nodes.AnnotationNodeInfo
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.ValidationAnnotation

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
abstract class AbstractDefaultDisposableTableModel(
  val table: java.util.Vector[java.util.Vector[String]],
  val columns: java.util.Vector[String] )
  extends DefaultTableModel( table, columns )
  with AbstractDisposableTableModel 