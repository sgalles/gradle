package org.gradle.cache.internal.locklistener;

import org.gradle.api.Action;

import java.io.File;

/**
 * By Szczepan Faber on 5/28/13
 */
public interface FileLockListener {
    int getPort();

    void stop();

    void lockCreated(File target, Action<File> whenContended);

    void lockClosed(File target);
}
