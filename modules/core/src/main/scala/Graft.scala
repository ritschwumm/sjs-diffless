package sjs.diffless

/** tells an element to export an attachment point for special logic */
final case class Graft[-N,+H](create:N=>H) extends Child[N,Any,Nothing,H]
