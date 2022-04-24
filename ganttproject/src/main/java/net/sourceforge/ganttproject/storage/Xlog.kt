/*
Copyright 2022 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject Cloud.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.storage

/**
 * Data for the initialization of the Colloboque database.
 */
data class InitRecord(
  val userId: String,
  val projectRefid: String,
  val projectXml: String
)

/**
 * Xlog transaction which consists of 1+ SQL statements.
 */
data class XlogRecord(
  val sqlStatements: List<String>
)

/**
 * Xlog from the specified client, which contains changes to the specified project
 * and consists of 1+ transaction.
 *
 * The base transaction ID is the identifier of the database state which the client was last synced with, as
 * reported by the server. It is assumed that the first transaction from the list is applied to the state identified
 * by baseTxnId, and i-th transaction from the list is applied to the state produced by (i-1)th transaction from the
 * list.
 */
data class InputXlog(
  val baseTxnId: String,
  val userId: String,
  val projectRefid: String,
  val transactions: List<XlogRecord>
)