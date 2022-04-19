package cloud.ganttproject.colloboque

data class InitRecord(
  val userId: String,
  val projectRefid: String,
  val payload: String
)
data class XlogRecord(
  val baseTxnId: String,
  val sqlStatements: List<String>
)

data class ClientXlog(
  val userId: String,
  val projectRefid: String,
  val xlogRecords: List<XlogRecord>
)