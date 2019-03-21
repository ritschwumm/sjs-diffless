package sjs.diffless

/**
 * parent class to everything that can be put inside a View mostly exists to guide scala's type inference
 * N is the Node subtype
 * M is the model type
 * A is the action type
 * H is the handle type
 */
trait Child[-N,-M,+A,+H]
