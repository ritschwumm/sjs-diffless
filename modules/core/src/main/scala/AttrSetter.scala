package sjs.diffless

/**
 * type class to associate an attribute name with its setter
 * this is necessary because one and the same attribute name can belong
 * to different types of elements and attribute values
 */
final case class AttrSetter[K,-N,-M](proc:(N,M)=>Unit)
