package sjs.diffless

/**
 * type class to associate an attribute name with its setter
 * this is necessary because one and the same attribute name can belong
 * to different types of elements and attribute values.
 *
 * if the getter exists, it is used to prevent updates when the last-set
 * value differs from the element's attribute value.
 * this can be used to avoid race-conditions when asynchronously updating
 * input fields: if this happens, they set the cursor to the wrong place.
 *
 */
final case class AttributeAccess[K,-N,M](setter:(N,M)=>Unit)
