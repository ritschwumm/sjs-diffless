package sjs.diffless.demo

import sjs.diffless.EventFlow
import sjs.diffless.demo.util.*

/** transforms the application state for each requested action */
object Controller {
	def execute(model:Model, action:Action, handles:Vector[Handle]):(Model,EventFlow)	= {
		val next:Model	=
			action match {
				case Action.Skip	=>
					// do nothing
					model

				case Action.Boot	=>
					// TODO ugly side effects

					handles collect	{ case Handle.CreateText(focus) => focus } foreach runNow

					Persistence.load() getOrElse model

				case Action.Complete	=>
					execute(model) {
						dependent(
							_.allCompleted,
							(current:Boolean) => {
								(Model.M.tasks >=> Mod.each >=> Task.M.data >=> TaskData.M.completed).set(!current)
							}
						)
					}
				case Action.Creating(text)	=>
					execute(model) {
						Model.M.creating.set(text)
					}

				case Action.Create	=>
					execute(model) {
						dependent(
							model	=> {
								if (model.creating.nonEmpty)	Some(newTask(model.creating))
								else							None
							},
							(taskOpt:Option[Task]) => {
								Model.M.tasks.lift(_ ++ taskOpt.toVector) `andThen`
								Model.M.creating.set("")
							}
						)
					}

				case Action.Task(id, TaskAction.Skip)	=>
					// do nothing
					model

				case Action.Task(id, TaskAction.Toggle)	=>
					execute(model) {
						(oneTask(id) >=> TaskData.M.completed).lift(!_)
					}

				case Action.Task(id, TaskAction.Edit) =>
					// TODO ugly side effects

					// NOTE timer delay is necessary because the element is still display:none here
					handles collect	{ case Handle.Task(`id`, TaskHandle.Editor(focus)) => focus } foreach runLater

					execute(model) {
						oneTask(id).lift(TaskData.M.editing.set(true) `andThen` previewFromText)
					}

				case Action.Task(id, TaskAction.Change(preview))	=>
					execute(model) {
						(oneTask(id) >=> TaskData.M.preview).set(preview)
					}

				case Action.Task(id, TaskAction.Commit)	=>
					execute(model) {
						oneTask(id).lift(TaskData.M.editing.set(false) `andThen` textFromPreview)
					}

				case Action.Task(id, TaskAction.Rollback) =>
					execute(model) {
						(oneTask(id) >=> TaskData.M.editing).set(false)
					}

				case Action.Task(id, TaskAction.Remove)	=>
					execute(model) {
						Model.M.tasks.lift(_.filter(_.id != id))
					}

				case Action.Filter(state)	=>
					execute(model) {
						Model.M.filter.set(state)
					}

				case Action.Clear	=>
					execute(model) {
						Model.M.tasks.lift (_.filter(!_.data.completed))
					}
			}

		// BETTER only when the model really changed
		Persistence.save(next)

		next -> EventFlow.permit
	}

	private def execute(model:Model)(func:Model=>Model):Model	=
		func(model)

	private val previewFromText:TaskData=>TaskData	= task => task.copy(preview	= task.text)
	private val textFromPreview:TaskData=>TaskData	= task => task.copy(text	= task.preview)

	private def oneTask(id:TaskId):Mod[Model,TaskData]	=
		Model.M.tasks >=> Mod.each >=> Task.M.whereId(id) >=> Task.M.data

	private def newTask(text:String):Task	=
		Task(
			id		= TaskId.create(),
			data	= TaskData(
				text		= text,
				completed	= false,
				editing		= false,
				preview		= ""
			)
		)
}
