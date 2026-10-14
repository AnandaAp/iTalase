package com.anlian.italase.shared.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun startOfLocalDayMillis(millis: Long): Long {
    val date: NSDate = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    val start: NSDate =
        NSCalendar.currentCalendar.startOfDayForDate(date) ?: date
    return (start.timeIntervalSince1970 * 1000).toLong()
}

actual fun formatDateTime(millis: Long): String {
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale(localeIdentifier = "id_ID")
    formatter.dateFormat = "d MMM yyyy • HH:mm"
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)) ?: ""
}
