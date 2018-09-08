package sjs.diffless

object handles extends handles

trait handles {
	def handle[N,H](create:N=>H):Handle[N,H]	= Handle apply		create
	def handle_[N,H](value:H):Handle[N,H]		= Handle constant	value
}
