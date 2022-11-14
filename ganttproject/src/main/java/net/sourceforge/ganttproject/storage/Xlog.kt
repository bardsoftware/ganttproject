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

@Serializable
sealed class OperationDto {
  @Serializable
  data class InsertOperationDto(
    val tableName: String,
    val values: Map<String, String?>
  ): OperationDto()

  @Serializable
  data class UpdateOperationDto(
    val tableName: String,
    val updateBinaryConditions: MutableList<Triple<String, BinaryPred, String>>,
    val updateRangeConditions: MutableList<Triple<String, RangePred, List<String>>>,
    val newValues: MutableMap<String, String>
  ): OperationDto()

  @Serializable
  data class DeleteOperationDto(
    val tableName: String,
    val deleteBinaryConditions: List<Triple<String, BinaryPred, String>> = listOf(), // [(fieldName, predicate, value)] eg [('foo', EQ, 'bar')]
    val deleteRangeConditions: List<Triple<String, RangePred, List<String>>> = listOf(),
  ): OperationDto()

  @Serializable
  data class MergeOperationDto(
    val tableName: String,
    val mergeBinaryConditions: List<Triple<String, BinaryPred, String>>, // [(fieldName, predicate, value)] eg [('foo', EQ, 'bar')]
    val mergeRangeConditions: List<Triple<String, RangePred, List<String>>>,
    val whenMatchedThenUpdate: Map<String, String>,
    val whenNotMatchedThenInsert: Map<String, String>
  ): OperationDto()
}

@Serializable
enum class BinaryPred {
  EQ, GT, LT, LE, GE
}

@Serializable
enum class RangePred {
  IN, NOT_IN
}

/**
 * Xlog transaction which consists of 1+ SQL statements.
 */
@Serializable
data class XlogRecord(
  val colloboqueOperations: List<OperationDto>
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
  val transactions: List<XlogRecord>,
  val clientTrackingCode: String
)

@Serializable
sealed class ServerResponse {
  /**
   * Response from the server, which contains the result of applying changes sent by the client.
   *
   * @param baseTxnId the transaction to which the changes were applied.
   * @param newBaseTxnId the resulting transaction.
   * @param logRecords the transaction itself, replicated back to client
   *
   * TODO: Provide transaction conflicts handling.
   */
  @Serializable
  data class CommitResponse(
    val baseTxnId: String,
    val newBaseTxnId: String,
    val projectRefid: String,
    val logRecords: List<XlogRecord>,
    val clientTrackingCode: String
  ) : ServerResponse()

  /**
   * Response from the server that signals about transaction commit failure.
   * */
  @Serializable
  data class ErrorResponse(
    val baseTxnId: String,
    val projectRefid: String,
    val message: String
  ) : ServerResponse()
}
