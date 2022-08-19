package sjs.diffless.demo

/** every action the user can request */
enum Action {
	case Skip
	case Boot

	case Complete
	case Creating(text:String)
	case Create

	case Task(id:TaskId, sub:TaskAction)

	case Filter(state:Option[Boolean])
	case Clear
}
