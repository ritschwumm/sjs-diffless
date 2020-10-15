package sjs.diffless

/** tells an element to export an attachment point for special logic */
final case class Export[-N,+H](create:N=>H) extends Child[N,Any,Nothing,H] {
	// TODO do we need this?
	def mapHandle[HH](func:H=>HH):Export[N,HH]	=
		Export(it => func(create(it)))
}
