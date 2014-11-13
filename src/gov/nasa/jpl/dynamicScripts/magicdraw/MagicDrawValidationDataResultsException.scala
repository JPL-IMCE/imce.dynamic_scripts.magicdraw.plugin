package gov.nasa.jpl.dynamicScripts.magicdraw

/** Wraps a [[MagicDrawValidationDataResults]] as a [[RuntimeException]]
 *  
 * Typically used for returning a value for a method typed as 'Try[...]'.
 * Given 'r', a [[MagicDrawValidationDataResults]], 
 * return 'MagicDrawValidationDataResultsException( r )' instead of 'Failure(...)'
 * 
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class MagicDrawValidationDataResultsException( validationDataResults: MagicDrawValidationDataResults ) 
extends RuntimeException( validationDataResults.title )

