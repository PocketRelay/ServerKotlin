package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.utils.Labels


open class Tdf(val label: String, val type: Byte) {

    val tag: Int = Labels.toTag(label)



}