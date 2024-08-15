package sjs.diffless.demo

object Model {
	val initial	=
		Model(
			creating	= "",
			tasks		= Vector.empty,
			filter		= None
		)

	object M {
		val creating:Mod[Model,String]			= Mod { func => model => model.copy(creating	= func(model.creating))	}
		val tasks:Mod[Model,Vector[Task]]		= Mod { func => model => model.copy(tasks		= func(model.tasks))	}
		val filter:Mod[Model,Option[Boolean]]	= Mod { func => model => model.copy(filter		= func(model.filter))	}
	}
}

/** our application's full state tree */
final case class Model(
	creating:String,
	tasks:Vector[Task],
	filter:Option[Boolean]
) {
	val incompleteCount:Int	=
		tasks.count(!_.data.completed)

	val hasCompleted:Boolean	=
		tasks.exists(_.data.completed)

	val allCompleted:Boolean	=
		tasks.forall(_.data.completed)

	val visible:Vector[Task]	=
		tasks.filter { item =>
			filter.forall(item.data.completed == _)
		}
}
