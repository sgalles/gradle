package org.gradle.cache.internal;

/**
 * By Szczepan Faber on 5/23/13
 */
public interface ThreadLock {

    void takeOwnership(String operationDisplayName);

    void releaseOwnership(String operationDisplayName);

}
