package org.gradle.cache.internal.locklistener;

import org.gradle.api.Action;

import java.io.File;

/**
 * By Szczepan Faber on 5/28/13
 */
public class NoOpFileLockListener implements FileLockListener {

    public int getPort() {
        return -1;
    }

    public void stop() {}

    public void lockCreated(File target, Action<File> whenContended) {}

    public void lockClosed(File target) {}
}
