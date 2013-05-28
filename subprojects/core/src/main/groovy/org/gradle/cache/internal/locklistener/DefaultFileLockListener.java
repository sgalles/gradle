package org.gradle.cache.internal.locklistener;

import org.gradle.api.Action;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.cache.internal.FileLockCommunicator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
* By Szczepan Faber on 5/28/13
*/
public class DefaultFileLockListener implements FileLockListener {
    private static final Logger LOGGER = Logging.getLogger(DefaultFileLockListener.class);
    private final Lock lock = new ReentrantLock();
    private final Map<File, Action<File>> contendedActions = new HashMap();
    private FileLockCommunicator communicator = new FileLockCommunicator();
    private boolean stopped;

    private Runnable listener;

    private Runnable newListener() {
        return new Runnable() {
            public void run() {
                try {
                    LOGGER.lifecycle("Starting file lock listener thread.");
                    doRun();
                } catch (Throwable t) {
                    LOGGER.lifecycle("Problems handling incoming cache access requests.", t);
                } finally {
                    LOGGER.lifecycle("File lock listener thread completed.");
                }
            }

            private void doRun() {
                while(!stopped) {
                    File requestedFileLock = communicator.receive();
                    lock.lock();
                    try {
                        if (stopped || contendedActions.isEmpty()) {
                            return;
                        }
                        Action<File> action = contendedActions.get(requestedFileLock);
                        if (action != null) {
                            action.execute(requestedFileLock);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }
        };
    }

    public int getPort() {
        return communicator.getPort();
    }

    public void stop() {
        lock.lock();
        try {
            stopped = true;
            contendedActions.clear();
            communicator.stop();
        } finally {
            lock.unlock();
        }
    }

    public void lockCreated(File target, Action<File> whenContended) {
        lock.lock();
        try {
            if (stopped) {
                throw new IllegalStateException("The listener was already stopped!");
            }
            contendedActions.put(target, whenContended);
            if (listener == null) {
                listener = newListener();
                new Thread(listener).start();
            }
        } finally {
            lock.unlock();
        }
    }

    public void lockClosed(File target) {
        lock.lock();
        try {
            if (stopped) {
                throw new IllegalStateException("The listener was already stopped!");
            }
            if (listener == null) {
                throw new IllegalStateException("Lock creation event was not received first!");
            }
            contendedActions.remove(target);
            if (contendedActions.isEmpty()) {
                communicator.stop();
            }
        } finally {
            lock.unlock();
        }
    }
}
