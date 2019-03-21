package sjs.diffless

object imports extends imports

/** merges all imports provided by diffless */
trait imports
	extends syntax
	with views
	with grafts
	with tags
	with attributes
	with setters
	with derived
	with emits
