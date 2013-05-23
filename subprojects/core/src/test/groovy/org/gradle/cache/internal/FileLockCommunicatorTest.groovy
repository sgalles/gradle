package org.gradle.cache.internal

import org.gradle.test.fixtures.ConcurrentTestUtil
import org.gradle.util.ConcurrentSpecification
import spock.lang.Specification

import static org.gradle.test.fixtures.ConcurrentTestUtil.poll

/**
 * By Szczepan Faber on 5/23/13
 */
class FileLockCommunicatorTest extends ConcurrentSpecification {

    def communicator = new FileLockCommunicator()
    File receivedFile
    File actualFile = new File("foo")

    def "can receive file"() {
        start {
            communicator.start()
            receivedFile = communicator.receive()
        }

        poll {
            assert communicator.getPort() != -1 && receivedFile == null
        }

        when:
        FileLockCommunicator.pingOwner("" + communicator.getPort(), actualFile)

        then:
        poll {
            assert receivedFile == actualFile.absoluteFile
        }
    }

    def "can be stopped"() {
        start {
            communicator.start()
            communicator.receive()
        }

        sleep(300)

        when:
        communicator.stop()

        then:
        finished()
    }
}
