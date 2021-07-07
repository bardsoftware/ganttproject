package biz.ganttproject.ganttview

import biz.ganttproject.ganttview.NewTaskState.*
import com.google.common.collect.Queues
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.TreeItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

sealed class NewTaskMsg<T>
data class TaskReady<T>(val task: T) : NewTaskMsg<T>()
data class TreeItemReady<T>(val treeItem: TreeItem<T>) : NewTaskMsg<T>()
//data class TreeRowReady<T>(val treeRow: TreeTableRow<T>) : NewTaskMsg<T>()
class EditingCompleted<T> : NewTaskMsg<T>()

sealed class NewTaskActorCommand<T>
class StartEditing<T>(val treeItem: TreeItem<T>) : NewTaskActorCommand<T>()
class CommitEditing<T>(val treeItem: TreeItem<T>) : NewTaskActorCommand<T>()

enum class NewTaskState { IDLE, TASK_READY, TREE_ITEM_READY, EDIT_STARTING, EDIT_COMPLETING }

class NewTaskActor<T> {
  val inboxChannel = Channel<NewTaskMsg<T>>()
  val commandChannel = Channel<NewTaskActorCommand<T>>()
  var state: NewTaskState = IDLE
  set(value) {
    println("State $field => $value")
    field = value
    canAddTask.value = value == IDLE
  }
  val canAddTask = SimpleBooleanProperty(true)

  var newTask: T? = null
  private var newTreeItem: TreeItem<T>? = null
  private val inboxQueue = Queues.newConcurrentLinkedQueue<NewTaskMsg<T>>()
  fun start() = GlobalScope.launch(Executors.newFixedThreadPool(1).asCoroutineDispatcher()) {
    for (msg in inboxChannel) {
      processMessage(msg)
    }
  }

  private suspend fun processQueue() {
    do {
      val msg = inboxQueue.poll() ?: break
      processMessage(msg)
    } while (true)
  }

  private suspend fun processMessage(msg: NewTaskMsg<T>) {
    println(msg)
    when (msg) {
      is TaskReady -> {
        when (state) {
          EDIT_COMPLETING -> {
            inboxQueue.add(msg)
          }
          IDLE, TASK_READY, TREE_ITEM_READY -> {
            newTask = msg.task
            state = TASK_READY
          }
          EDIT_STARTING -> {
            inboxQueue.add(msg)
            commandChannel.send(CommitEditing(newTreeItem!!))
            state = EDIT_COMPLETING
          }
        }
      }
      is TreeItemReady -> {
        when (state) {
          IDLE -> TODO("this must be error: we received a tree item prior to new task")
          TASK_READY -> {
            if (msg.treeItem.value === newTask) {
              newTreeItem = msg.treeItem
              state = EDIT_STARTING
              commandChannel.send(StartEditing(msg.treeItem))
            } else {
              // This might be a tree item created for the previous task, if user hits INSERT quick enough
              // We can just ignore this message.
            }
          }
          TREE_ITEM_READY, EDIT_STARTING -> {
            TODO("this must be error: we received a new tree item when we are editing or about to start editing")
          }
          EDIT_COMPLETING -> inboxQueue.add(msg)
        }
      }
      is EditingCompleted -> {
        when (state) {
          EDIT_STARTING, EDIT_COMPLETING -> {
            newTask = null
            newTreeItem = null
            state = IDLE
            processQueue()
          }
          IDLE -> {}
          else -> TODO("this must be error: editing completed when state is $state")
        }
      }
    }
  }
}
