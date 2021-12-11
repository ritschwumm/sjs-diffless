package sjs.diffless.demo

import sjs.diffless.*
import sjs.diffless.imports.{ given, * }

/** all our views in one place */
object Views {
	lazy val mainView:View[Model,Action,Handle]	=
		div(
			className	:= "main",
			headerView,
			taskListView(_.visible),
			footerView
		)

	lazy val headerView:View[Model,Action,Handle]	=
		header(
			className	:= "header",
			completeView(_.tasks),
			createView
		)

	lazy val completeView:View[Vector[Task],Action,Handle]	=
		input(
			className	:= "complete",
			`type`		:= "checkbox",
			visible		~= (_.nonEmpty),
			checked		~= (_ forall (_.data.completed)),
			onInput		|= { (target, event) => Action.Complete }
		)

	lazy val createView:View[Model,Action,Handle]	=
		input(
			className	:= "create",
			`type`		:= "text",
			placeholder	:= "what needs to be done?",
			value		~= (_.creating),
			onInput		|= { (target, event) => Action.Creating(target.value)								},
			onKeyUp		|= { (target, event) => if (event.key == "Enter") Action.Create else Action.Skip	},
			attach { self =>
				Handle.CreateText(
					focus	= () => self.focus()
				)
			}
		)

	lazy val taskListView:View[Vector[Task],Action,Handle]	=
		ul(
			className	:= "task-list",
			taskItemViewEmbed
		)

	lazy val taskItemViewEmbed:View[Vector[Task],Action,Handle]	= {
		val keyify:Task=>(String,Task)	= it => it.id.value -> it
		keyed(taskItemEmbeddedView)(_ map keyify)
	}

	lazy val taskItemEmbeddedView:View[Task,Action,Handle]	=
		taskItemView
		.contextual (
			actionFunc	= Action.Task.apply,
			handleFunc	= Handle.Task.apply
		)
		.adaptModel { task =>
			task.id -> task.data
		}

	lazy val taskItemView:View[TaskData,TaskAction,TaskHandle]	=
		li(
			className	:= "task-item",
			onDblClick	|= { (target, model) => TaskAction.Edit },
			input(
				className	:= "task-item-active",
				`type`		:= "checkbox",
				visible		~= (!_.editing),
				checked		~= (_.completed),
				onInput		|= { (target, event) => TaskAction.Toggle }
			),
			div(
				classSet	~= { task =>
					if (task.completed) Set("task-item-text", "task-item-text-completed")
					else				Set("task-item-text")
				},
				displayed	~= (!_.editing),
				text(_.text)
			),
			button(
				className	:= "task-item-remove",
				displayed	~= (!_.editing),
				onClick		|= { (target, event) => TaskAction.Remove },
				literal("🗙")
			),
			input(
				className	:= "task-item-editor",
				`type`		:= "text",
				displayed	~= (_.editing),
				value		~= (_.preview),
				onInput		|= { (target, event) => TaskAction.Change(target.value)	},
				onBlur		|= { (target, event) => TaskAction.Commit				},
				onKeyUp		|= { (target, event) =>
						 if (event.key == "Enter") 	TaskAction.Commit
					else if (event.key == "Escape")	TaskAction.Rollback
					else							TaskAction.Skip
				},
				attach { self =>
					TaskHandle.Editor(
						() => self.focus()
					)
				}
			)
		)

	lazy val footerView:View[Model,Action,Nothing]	=
		footer(
			className	:= "footer",
			displayed	~= (_.tasks.nonEmpty),
			countView(_.incompleteCount),
			filterListView(_.filter),
			clearView(_.hasCompleted)
		)

	lazy val countView:View[Int,Action,Nothing]	=
		div(
			className	:= "count",
			strong(
				text(_.toString)
			),
			literal(" "),
			text(it =>
				if (it == 1) "item" else "items"
			),
			literal(" left")
		)

	lazy val filterListView:View[Option[Boolean],Action,Nothing]	=
		div(
			className	:= "filter-list",
			filterItemView("all",		None),
			filterItemView("active",	Some(false)),
			filterItemView("completed",	Some(true))
		)

	def filterItemView(labelText:String, state:Option[Boolean]):View[Option[Boolean],Action,Nothing]	=
		label(
			className	:= "filter-item",
			input(
				`type`	:= "radio",
				checked	~= (_ == state),
				onInput	|= { (target, event) => Action.Filter(state) }
			),
			literal(labelText)
		)

	lazy val clearView:View[Boolean,Action,Nothing]	=
		button(
			className	:= "clear",
			visible		~= identity,
			onClick		|= { (target, event) => Action.Clear },
			literal("clear completed")
		)
}
