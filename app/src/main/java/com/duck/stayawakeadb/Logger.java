package com.duck.stayawakeadb;

import com.duck.flexilogger.FlexiLog;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Bradley Duck on 2019/03/04.
 */
public class Logger extends FlexiLog {
    @Override
    public boolean canLogToConsole(int i) {
        return true;
    }

    @Override
    public boolean mustReport(int i) {
        return false;//no reporting
    }

    @Override
    public void report(int i, @NotNull String s, @NotNull String s1) {
        //no reporting
    }

    @Override
    public void report(int i, @NotNull String s, @NotNull String s1, @NotNull Throwable throwable) {
        //no reporting
    }
}
