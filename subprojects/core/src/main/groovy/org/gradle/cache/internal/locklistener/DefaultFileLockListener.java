package org.gradle.cache.internal.locklistener;

import org.gradle.api.Action;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.cache.internal.FileLockCommunicator;
import org.gradle.internal.concurrent.DefaultExecutorFactory;
import org.gradle.internal.concurrent.StoppableExecutor;

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

    private Runnable listener() { return new Runnable() {
        public void run() {
            try {
                LOGGER.lifecycle("Starting file lock listener thread.");
                doRun();
                assertState();
            } catch (Throwable t) {
                LOGGER.lifecycle("Problems handling incoming cache access requests.", t);
            } finally {
                LOGGER.lifecycle("File lock listener thread completed.");
            }
        }

        private void assertState() {
            if (communicator.getPort() != -1) {
                throw new IllegalStateException("Socket was not closed correctly!");
            }
        }

        private void doRun() {
            File requestedFileLock;
            while((requestedFileLock = communicator.receive()) != null) {
                lock.lock();
                Action<File> action;
                try {
                    action = contendedActions.get(requestedFileLock);
                } finally {
                    lock.unlock();
                }
                if (action != null) {
                    action.execute(requestedFileLock);
                }
            }
        }
    };}

    private StoppableExecutor executor;

    public void lockCreated(File target, Action<File> whenContended) {
        lock.lock();
        try {
            if (contendedActions.isEmpty()) {
                LOGGER.lifecycle("Starting communicator because first cache opens {}", target);
                communicator.start();
                executor = new DefaultExecutorFactory().create("Listen for file lock access requests from other processes");
                executor.execute(listener());
            }
            contendedActions.put(target, whenContended);
        } finally {
            lock.unlock();
        }
    }

    public void lockClosed(File target) {
        lock.lock();
        try {
            contendedActions.remove(target);
            if (contendedActions.isEmpty()) {
                LOGGER.lifecycle("Stopping receiver, last cache is being closed {}", target);
                communicator.stop();
                executor.requestStop();
            }
        } finally {
            lock.unlock();
        }
    }

    public int reservePort() {
        lock.lock();
        try {
            if (!communicator.isStarted()) {
                communicator.start();
            }
            return communicator.getPort();
        } finally {
            lock.unlock();
        }
    }
}
