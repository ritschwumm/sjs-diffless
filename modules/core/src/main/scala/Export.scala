package sjs.diffless

/** tells an element to export an attachment point for special logic */
final case class Export[-N,+H](create:N=>H) extends Child[N,Any,Nothing,H]
