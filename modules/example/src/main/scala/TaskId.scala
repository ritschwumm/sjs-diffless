package sjs.diffless.demo

object TaskId {
	def create():TaskId	= TaskId(System.currentTimeMillis().toString)
}

final case class TaskId(value:String)
