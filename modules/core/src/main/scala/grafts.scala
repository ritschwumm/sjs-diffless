package sjs.diffless

object grafts extends grafts

trait grafts {
	def graft[N,H](create:N=>H):Graft[N,H]		= Graft apply	create
}
