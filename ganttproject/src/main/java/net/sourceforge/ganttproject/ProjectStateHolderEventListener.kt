package net.sourceforge.ganttproject

import biz.ganttproject.app.Barrier
import biz.ganttproject.app.BarrierEntrance
import net.sourceforge.ganttproject.ProjectEventListener.Stub
import net.sourceforge.ganttproject.storage.ProjectStateStorage
import net.sourceforge.ganttproject.storage.ProjectStateStorageFactory

/**
 * Holds the current state of a Gantt project, updating it on events.
 *
 * @param storageFactory - factory for generating a project state storage.
 */
class ProjectStateHolderEventListener(private val storageFactory: ProjectStateStorageFactory) : Stub() {
  private var stateStorage: ProjectStateStorage? = null

  override fun projectOpened(barrierRegistry: BarrierEntrance?, barrier: Barrier<IGanttProject>?) {
    stateStorage = storageFactory.getStorage()
    // ...
  }

  override fun projectClosed() {
    // ...
    stateStorage?.shutdown()
  }
}