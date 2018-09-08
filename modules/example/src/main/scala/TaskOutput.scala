package sjs.diffless.demo

object TaskOutput {
	final case class Editor(focus:()=>Unit)	extends TaskOutput
}

sealed trait TaskOutput
