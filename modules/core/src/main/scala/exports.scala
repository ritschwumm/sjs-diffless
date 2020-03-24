package sjs.diffless

object exports extends exports

/** syntax for grafting points */
trait exports {
	def export[N,H](create:N=>H):Export[N,H]	= Export apply	create
}
