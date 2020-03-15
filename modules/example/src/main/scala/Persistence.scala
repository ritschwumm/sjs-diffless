package sjs.diffless.demo

import scala.scalajs.js
import org.scalajs.dom.ext.LocalStorage

object Persistence {
	private val key	= "todomvc-diffless"

	def load():Option[Model]	= {
		LocalStorage(key) map {	str =>
			val json	= js.JSON parse str
			readModel(json)
		}
	}

	private def readModel(json:js.Dynamic):Model	=
		Model(
			creating	= json.creating.asInstanceOf[String],
			tasks		= json.tasks.asInstanceOf[js.Array[js.Dynamic]].toArray[js.Dynamic].toVector map readTask,
			filter		= if (json.filter == null) None else Some(json.filter.asInstanceOf[Boolean])
		)

	private def readTask(json:js.Dynamic):Task	=
		Task(
			id		= TaskId(json.id.asInstanceOf[String]),
			data	= TaskData(
				text		= json.text.asInstanceOf[String],
				completed	= json.completed.asInstanceOf[Boolean],
				editing		= json.editing.asInstanceOf[Boolean],
				preview		= json.preview.asInstanceOf[String]
			)
		)

	//------------------------------------------------------------------------------

	def save(model:Model):Unit	= {
		val json	= writeModel(model)
		val str		= js.JSON stringify json
		LocalStorage(key)	= str
	}

	private def writeModel(model:Model):js.Dynamic	=
		js.Dynamic.literal(
			creating	= model.creating,
			tasks		= js.Array((model.tasks map writeTask):_*),
			filter		= model.filter map (js.Any.fromBoolean) getOrElse null
		)

	private def writeTask(task:Task):js.Dynamic	=
		js.Dynamic.literal(
			id			= task.id.value,
			text		= task.data.text,
			completed	= task.data.completed,
			editing		= task.data.editing,
			preview		= task.data.preview
		)
}
