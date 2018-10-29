package sjs.diffless

final case class Graft[-N,+H](create:N=>H) extends Child[N,Any,Nothing,H]
