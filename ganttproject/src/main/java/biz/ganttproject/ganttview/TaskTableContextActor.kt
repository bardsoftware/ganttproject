package biz.ganttproject.ganttview

import biz.ganttproject.ganttview.NewTaskState.*
import javafx.scene.control.TreeItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.task.Task
import java.util.concurrent.Executors

sealed class NewTaskMsg
data class TaskReady(val task: Task) : NewTaskMsg()
data class TreeItemReady(val treeItem: TreeItem<Task>) : NewTaskMsg()
class EditingCompleted : NewTaskMsg()

sealed class NewTaskActorCommand
class StartEditing(val treeItem: TreeItem<Task>) : NewTaskActorCommand()
class CommitEditing(val treeItem: TreeItem<Task>) : NewTaskActorCommand()

enum class NewTaskState { IDLE, TASK_READY, TREE_ITEM_READY, EDIT_STARTING, EDIT_COMPLETING }

class NewTaskActor() {
  val inboxChannel = Channel<NewTaskMsg>()
  val commandChannel = Channel<NewTaskActorCommand>()
  var state: NewTaskState = IDLE
  set(value) {
    println("State $field => $value")
    field = value
  }
  var newTask: Task? = null
  var newTreeItem: TreeItem<Task>? = null
  val inboxQueue = mutableListOf<NewTaskMsg>()
  fun start() = GlobalScope.launch(Executors.newFixedThreadPool(1).asCoroutineDispatcher()) {
    for (msg in inboxChannel) {
      processMessage(msg)
    }
  }

  suspend fun processQueue() {
    inboxQueue.forEach { processMessage(it) }
    inboxQueue.clear()
  }

  suspend fun processMessage(msg: NewTaskMsg) {
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
          else -> TODO("this must be error: editing completed when state is $state")
        }
      }
    }
  }
}
