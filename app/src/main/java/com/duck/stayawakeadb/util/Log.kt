package com.duck.stayawakeadb.util

import com.duck.flexilogger.FlexiLog
import com.duck.flexilogger.LogType

object Log : FlexiLog() {
    /**
     * Used to determine if we should Lod to the console or not.
     */
    override fun canLogToConsole(type: LogType): Boolean {
        return true
    }

    /**
     * Used to determine if we should send a report (to Crashlytics or equivalent)
     */
    override fun shouldReport(type: LogType): Boolean {
        return false //no reporting
    }

    override fun shouldReportException(tr: Throwable): Boolean {
        return false
    }

    /**
     * Implement the actual reporting.
     *
     * @param type [Int] @[LogType], the type of log this came from.
     * @param tag [Class] The Log tag
     * @param msg [String] The Log message.
     */
    override fun report(type: LogType, tag: String, msg: String) {
        //no reporting
    }

    /**
     * Implement the actual reporting.
     *
     * @param type [Int] @[LogType], the type of log this came from.
     * @param tag [Class] The Log tag
     * @param msg [String] The Log message.
     * @param tr  [Throwable] to be attached to the Log.
     */
    override fun report(type: LogType, tag: String, msg: String, tr: Throwable) {
        //no reporting
    }
}