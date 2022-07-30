package com.jacobtread.kme.utils

import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.responseText
import com.jacobtread.xml.OutputOptions
import com.jacobtread.xml.XmlVersion
import com.jacobtread.xml.element.XmlRootNode

/**
 * responseXml Creates an XML response that initializes the xml node tree
 * using the provided init function which is passed the root node with the
 * provided node name as the receiver
 *
 * Automatically sets the encoding and XML version for the root node so
 * that that information is included in the built xml
 *
 * @param rootName The name of the root node
 * @param init The initializer function
 * @receiver The newly created root node
 * @return The HttpResponse created from the encoded XML
 */
inline fun responseXml(rootName: String, init: XmlRootNode.() -> Unit): HttpResponse {
    val rootNode = XmlRootNode(rootName)
    rootNode.encoding = "UTF-8"
    rootNode.version = XmlVersion.V10
    rootNode.init()
    val outputOptions = OutputOptions(
        prettyPrint = false
    )
    return responseText(rootNode.toString(outputOptions), "text/xml;charset=UTF-8")
}