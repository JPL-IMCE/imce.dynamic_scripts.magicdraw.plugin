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

package gov.nasa.jpl.dynamicScripts.magicdraw.validation

import gov.nasa.jpl.dynamicScripts.magicdraw.validation

/** Wraps a [[validation.MagicDrawValidationDataResults]] as a `java.lang.RuntimeException`
 *
 * Typically used for returning a value for a method typed as 'Try[...]'.
 * Given 'r', a [[validation.MagicDrawValidationDataResults]],
 * return 'MagicDrawValidationDataResultsException( r )' instead of 'Failure(...)'
 *
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 */
case class MagicDrawValidationDataResultsException
( validationDataResults: validation.MagicDrawValidationDataResults )
extends java.lang.RuntimeException( validationDataResults.title )