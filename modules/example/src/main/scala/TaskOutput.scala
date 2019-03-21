package sjs.diffless.demo

object TaskOutput {
	final case class Editor(focus:()=>Unit)	extends TaskOutput
}

/** DOM node markers belonging to a specific Task */
sealed trait TaskOutput
