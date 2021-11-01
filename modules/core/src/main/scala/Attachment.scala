package sjs.diffless

/** tells an element to attach a handle to itself point for special logic like calling .focus() or getting its bounds */
final case class Attachment[-N,+H](create:N=>H) extends Child[N,Any,Nothing,H] {
	def mapHandle[HH](func:H=>HH):Attachment[N,HH]	=
		Attachment(it => func(create(it)))
}
