package com.toasterofbread.spmp.resources.uilocalisation.localised

data class HMSData(val hours: String, val minutes: String, val seconds: String, val splitter: String = "")

fun getHoursMinutesSecondsSuffixes(hl: String): HMSData? =
    when (hl.split('-', limit = 2).first()) {
        "en" -> HMSData("hours", "minutes", "seconds", " ")
        "ja" -> HMSData("時間", "分", "秒")
        else -> null
    }