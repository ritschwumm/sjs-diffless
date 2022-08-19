package sjs.diffless.demo

/** DOM node markers belonging to a specific Task */
enum TaskHandle {
	case Editor(focus:()=>Unit)
}
