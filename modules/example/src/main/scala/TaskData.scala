package sjs.diffless.demo

object TaskData {
	object M {
		val completed:Mod[TaskData,Boolean]	= Mod { func => task => task copy (completed	= func(task.completed))	}
		val editing:Mod[TaskData,Boolean]	= Mod { func => task => task copy (editing		= func(task.editing))	}
		val preview:Mod[TaskData,String]	= Mod { func => task => task copy (preview		= func(task.preview))	}
	}
}

final case class TaskData(
	text:String,
	completed:Boolean,
	editing:Boolean,
	preview:String
)
