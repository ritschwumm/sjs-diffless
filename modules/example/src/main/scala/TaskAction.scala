package sjs.diffless.demo

enum TaskAction {
	case Skip
	case Toggle
	case Remove
	case Edit
	case Change(preview:String)
	case Commit
	case Rollback
}
