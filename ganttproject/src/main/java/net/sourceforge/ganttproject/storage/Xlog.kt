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

import kotlinx.serialization.Serializable

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
@Serializable
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
@Serializable
data class InputXlog(
  val baseTxnId: String,
  val userId: String,
  val projectRefid: String,
  val transactions: List<XlogRecord>
)

/**
 * Response from the server, which contains the result of applying changes sent by the client.
 *
 * @param baseTxnId the transaction to which the changes were applied.
 * @param newBaseTxnId the resulting transaction.
 *
 * TODO: Get rid of type. It's used for routing in `WebSocketClient::onMessage`.
 * TODO: Provide transaction conflicts handling.
 */
@Serializable
data class ServerCommitResponse(
  val baseTxnId: String,
  val newBaseTxnId: String,
  val projectRefid: String,
  val type: String
)

/** Response from the server that signals about transaction commit failure. */
@Serializable
data class ServerCommitError(
  val baseTxnId: String,
  val projectRefid: String,
  val message: String,
  val type: String
)

const val SERVER_COMMIT_RESPONSE_TYPE = "ServerCommitResponse"
const val SERVER_COMMIT_ERROR_TYPE = "ServerCommitError"