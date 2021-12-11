package sjs.diffless

/** syntax for attaching handles */
object attachments {
	def attach[N,H](create:N=>H):Attachment[N,H]	= Attachment(create)
}
