/*
Copyright 2022 BarD Software s.r.o., Anastasiia Postnikova

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
package biz.ganttproject.storage.cloud

import com.google.common.collect.ImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.*
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import org.apache.commons.lang3.tuple.ImmutablePair
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.swing.event.UndoableEditEvent

class ColloboqueClient(private val projectDatabase: ProjectDatabase, undoManager: GPUndoManager) {
  private val myBaseTxnCommitInfo = TxnCommitInfo("", -1)
  private var projectRefid: String? = null
  private val eventLoopScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
  private val channelScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
  private val internalChannel = Channel<List<XlogRecord>>()
  private val externalChannel = Channel<ServerResponse.CommitResponse>()

  init {
    undoManager.addUndoableEditListener(object: GPUndoListener {
      override fun undoableEditHappened(e: UndoableEditEvent) {
        sendProjectStateLogs()
      }

      override fun undoOrRedoHappened() {}
      override fun undoReset() {}
    })
    eventLoopScope.launch {
      runEventLoop(internalChannel, externalChannel)
    }
  }

  private suspend fun runEventLoop(internalChannel: Channel<List<XlogRecord>>, externalChannel: Channel<ServerResponse.CommitResponse>) {
    var acceptInternal = true
    while (true) {
      println("Next event loop cycle. Accept internal=$acceptInternal")
      if (acceptInternal) {
        select<Unit> {
          internalChannel.onReceive { txns ->
            println("Message from the internal channel")
            sendXlog(txns)
            // TODO: start timeout to report missing response in a few moments
            acceptInternal = false
            println("We will stop accepting internal messages until we hear a response")
          }
          externalChannel.onReceive { response ->
            println("Message from the EXTERNAL channel")
            receiveXlog(response)
            acceptInternal = true
          }
        }
      } else {
        val response = externalChannel.receive()
        println("Message from the EXTERNAL channel")
        receiveXlog(response)
        acceptInternal = true
      }
    }
  }

  private fun sendXlog(txns: List<XlogRecord>) {
    webSocket.sendLogs(
      InputXlog(
        myBaseTxnCommitInfo.get().left,
        "userId",
        projectRefid!!,
        txns,
        myBaseTxnCommitInfo.generateTrackingCode()
      )
    )
  }

  private fun receiveXlog(response: ServerResponse.CommitResponse) {
    try {
      // Check if we received our own update
      if (response.clientTrackingCode != myBaseTxnCommitInfo.trackingCode) {
        // This is not our own update so let's apply it
        projectDatabase.applyUpdate(response.logRecords[0])
      }
      myBaseTxnCommitInfo.update(response.baseTxnId, response.newBaseTxnId, 1)
    } catch (ex: Exception) {
      LOG.error("Failed to apply external update", ex)
    }
  }

  fun attach(webSocket: WebSocketClient) {
    webSocket.onCommitResponseReceived { response  -> this.fireXlogReceived(response) }
  }

  fun start(projectRefid: String, baseTxnId: String) {
    this.projectRefid = projectRefid
    onBaseTxnIdReceived(baseTxnId)
    this.projectDatabase.startLog(baseTxnId)
  }

  private fun fireXlogReceived(response: ServerResponse.CommitResponse) {
    channelScope.launch {
      externalChannel.send(response)
    }
  }

  private fun onBaseTxnIdReceived(baseTxnId: String) {
    myBaseTxnCommitInfo.update("", baseTxnId, 0)
  }

  // TODO: Accumulate changes instead of sending it every time.
  private fun sendProjectStateLogs() {
    LOG.debug("Sending project state logs")
    try {
      val baseTxnCommitInfo = myBaseTxnCommitInfo.get()
      val txns: List<XlogRecord> = projectDatabase.fetchTransactions(baseTxnCommitInfo.right + 1, 1)
      if (txns.isNotEmpty()) {
        channelScope.launch {
          internalChannel.send(txns)
        }
      }
    } catch (e: ProjectDatabaseException) {
      LOG.error("Failed to send logs", arrayOf<Any>(), ImmutableMap.of<String, Any>(), e)
    }
  }

}


/**
 * Holds the transaction ID specified by the server (String) and the local ID (Integer) of the last local transaction
 * committed by the server.
 * The local ID corresponds to the txn ID stored in the database.
 */
private class TxnCommitInfo(serverId: String, localId: Int) {
  private val myTxnId: AtomicReference<ImmutablePair<String, Int>>
  var trackingCode: String = ""
    private set

  init {
    myTxnId = AtomicReference(ImmutablePair(serverId, localId))
  }

  /** If `oldTxnId` is currently being hold, sets the txn ID to `newTxnId` and moves the local ID ahead by `committedNum`.  */
  fun update(oldTxnId: String, newTxnId: String, committedNum: Int) {
    myTxnId.updateAndGet { oldValue: ImmutablePair<String, Int> ->
      if (oldValue.left == oldTxnId) {
        return@updateAndGet ImmutablePair(newTxnId, oldValue.right + committedNum)
      } else {
        return@updateAndGet oldValue
      }
    }
  }

  fun get(): ImmutablePair<String, Int> {
    return myTxnId.get()
  }

  fun generateTrackingCode(): String =
    UUID.randomUUID().toString().replace("-", "").also {
      trackingCode = it
    }


}

private val LOG = GPLogger.create("Cloud.RealTimeSync")
