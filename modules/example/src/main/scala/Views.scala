package sjs.diffless.demo

//import org.scalajs.dom.raw._

import sjs.diffless._
import sjs.diffless.imports._

object Views {
	lazy val mainView:View[Model,Action,Output]	=
			div(
				className	:= "main",
				headerView,
				taskListView(_.visible),
				footerView
			)
			
	lazy val headerView:View[Model,Action,Output]	=
			header(
				className	:= "header",
				completeView(_.tasks),
				createView
			)
			
	lazy val completeView:View[Vector[Task],Action,Output]	=
			input(
				className	:= "complete",
				`type`		:= "checkbox",
				visible		~= (_.nonEmpty),
				checked		~= (_ forall (_.data.completed)),
				onInput		|= { (target, event) => Action.Complete }
			)
			
	lazy val createView:View[Model,Action,Output]	=
			input(
				className	:= "create",
				`type`		:= "text",
				placeholder	:= "what needs to be done?",
				value		~= (_.creating),
				onInput		|= { (target, event) => Action.Creating(target.value)							},
				onKeyUp		|= { (target, event) => if (event.key == "Enter") Action.Create else Action.Skip	},
				Handle { target =>
					Output.CreateText(
						focus	= () => target.focus()
					)
				}
			)
			
	lazy val taskListView:View[Vector[Task],Action,Output]	=
			ul(
				className	:= "task-list",
				taskItemViewEmbed
			)
			
	lazy val taskItemViewEmbed:View[Vector[Task],Action,Output]	= {
		val keyify:Task=>(String,Task)	= it => it.id.value -> it
		keyed(taskItemEmbeddedView) contraMapModel (_ map keyify)
	}
	
	// TODO let this use a TaskEntity
	lazy val taskItemEmbeddedView:View[Task,Action,Output]	=
			taskItemView
			.contextual (
				actionFunc	= Action.Task.apply,
				handleFunc	= Output.Task.apply
			)
			.contraMapModel { task =>
				task.id -> task.data
			}
		
	// TODO let this use a TaskData
	lazy val taskItemView:View[TaskData,TaskAction,TaskOutput]	=
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
					//className	:= "task-item-text",
					classSet	~= { task =>
						(if (task.completed) Set("task-item-text-completed") else Set.empty[String]) +
						"task-item-text"
					},
					display		~= (!_.editing),
					text(_.text)
				),
				button(
					className	:= "task-item-remove",
					display		~= (!_.editing),
					onClick		|= { (target, event) => TaskAction.Remove },
					text_("🗙")
				),
				input(
					className	:= "task-item-editor",
					`type`		:= "text",
					display		~= (_.editing),
					value		~= (_.preview),
					onInput		|= { (target, event) => TaskAction.Change(target.value)	},
					onBlur		|= { (target, event) => TaskAction.Commit				},
					onKeyUp		|= { (target, event) =>
							 if (event.key == "Enter") 	TaskAction.Commit
						else if (event.key == "Escape")	TaskAction.Rollback
						else							TaskAction.Skip
					},
					Handle { target =>
						TaskOutput.Editor(
							() => target.focus()
						)
					}
				)
			)
			
	lazy val footerView:View[Model,Action,Output]	=
			footer(
				className	:= "footer",
				display		~= (_.tasks.nonEmpty),
				countView(_.incompleteCount),
				filterListView(_.filter),
				clearView(_.hasCompleted)
			)
			
	lazy val countView:View[Int,Action,Output]	=
			div(
				className	:= "count",
				strong(
					text(_.toString)
				),
				text_(" "),
				text(it =>
					if (it == 1) "item" else "items"
				),
				text_(" left")
			)
			
	lazy val filterListView:View[Option[Boolean],Action,Output]	=
			div(
				className	:= "filter-list",
				filterItemView("all",		None),
				filterItemView("active",	Some(false)),
				filterItemView("completed",	Some(true))
			)
			
	def filterItemView(labelText:String, state:Option[Boolean]):View[Option[Boolean],Action,Output]	=
			label(
				className	:= "filter-item",
				input(
					`type`	:= "radio",
					checked	~= (_ == state),
					onInput	|= { (target, event) => Action.Filter(state) }
				),
				text_(labelText)
			)
			
	lazy val clearView:View[Boolean,Action,Output]	=
			button(
				className	:= "clear",
				visible		~= identity,
				onClick		|= { (target, event) => Action.Clear },
				text_("clear completed")
			)
}
