package sjs.diffless.demo

object Task {
	object M {
		val id:Mod[Task,TaskId]		= Mod { func => task => task copy (id	= func(task.id))	}
		val data:Mod[Task,TaskData]	= Mod { func => task => task copy (data	= func(task.data))	}
		
		def whereId(id:TaskId):Mod[Task,Task]	= Mod where (_.id == id)
	}
}

final case class Task(
	id:TaskId,
	data:TaskData
)
