package lab.reco.job

case class Task(id: String,
                children: Seq[Task] = Seq.empty,
                startedAt: Option[Long] = None,
                finishedAt: Option[Long] = None,
                failReason: Option[String] = None)


private[job] case class Node(id: String, var task: TaskMeta, var children: Seq[Node])
private[job] case class TaskMeta(startedAt: Option[Long], finishedAt: Option[Long], failReason: Option[String])


object Job {

  def build(task: Task): Job = {

    def traverse(task: Task): Node = {
      val children = task.children.map { traverse }
      Node(task.id, TaskMeta(task.startedAt, task.finishedAt, task.failReason), children)
    }

    Job(traverse(task))
  }

}


case class Job(root: Node) {

  private def updateTask(id: String, func: TaskMeta => TaskMeta): Unit = {

    def traverse(node: Node): Unit = {
      if (node.id == id) {
        node.task = func(node.task)
      } else {
        node.children.foreach(traverse)
      }
    }

    traverse(root)
  }

  def startTask(id: String, time: Long): Unit =
    updateTask(id, _.copy(startedAt = Some(time)))

  def succeedTask(id: String, time: Long): Unit =
    updateTask(id, _.copy(finishedAt = Some(time)))

  def failTask(id: String, time: Long, reason: String): Unit =
    updateTask(id, _.copy(finishedAt = Some(time), failReason = Some(reason)))

  def currentStatus: Task = {
    def traverse(node: Node): Task = {
      val children = node.children.map { traverse }
      Task(node.id, children, node.task.startedAt, node.task.finishedAt, node.task.failReason)
    }

    traverse(root)
  }

  def addTask(id: String, parentId: String): Unit = {
    val theTask = Node(id, TaskMeta(None, None, None), Seq.empty)

    def traverse(node: Node, searchId: String): Unit = {
      if (node.id == searchId) {
        node.children :+= theTask
      } else {
        node.children.foreach { traverse(_, searchId) }
      }
    }

    traverse(root, parentId)
  }

  def isFinished: Boolean = {

    def traverse(node: Node): Boolean = {
      if (node.task.finishedAt.isEmpty) {
        false
      } else {
        node.children.forall { traverse }
      }
    }

    traverse(root)
  }

  def isFailed: Boolean = {

    def traverse(node: Node): Boolean = {
      if (node.task.failReason.isDefined) {
        true
      } else {
        node.children.exists { traverse }
      }
    }

    traverse(root)
  }
}