package sjs.diffless

object imports extends imports

/** merges all imports provided by diffless */
trait imports
	extends syntax
	with views
	with tags
	with attributes
	with derived
	with setters
	with emits
	with attachments
