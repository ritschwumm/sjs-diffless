package sjs.diffless

object Handle {
	def constant[N,H](value:H):Handle[N,H]	=
			Handle((_) => value)
}

final case class Handle[-N,+H](create:N=>H) extends Child[N,Any,Nothing,H] {
	/*
	def mapHandle[HH](func:H=>HH):Handle[N,HH]	=
			Handle { node =>
				func(create(node))
			}
	*/
}
