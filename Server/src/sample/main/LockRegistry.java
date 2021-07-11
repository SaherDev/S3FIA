package sample.main;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public enum LockRegistry {


    INSTANCE;

    // map of file names to locks
    private Map<String, ReadWriteLock> lockMap = new HashMap<String, ReadWriteLock>();

    // lock to protect our registry and helps to prevent multiple threads
    // from instantiating a lock with the same key
    private Lock registryLock = new ReentrantLock();

    public enum LockType {
        READ, WRITE
    }

    /**
     *  acquire lock for file
     * @param fileName file name (path/file)
     * @param type read/write
     */
    public void acquire(String fileName, LockType type) {

        // lazily instantiates locks on first use
        ReadWriteLock lock = retrieveLock(fileName);

        switch (type) {
            case READ:
                lock.readLock().lock();
                break;
            case WRITE:
                lock.writeLock().lock();
                break;
            default:

                break;
        }

    }

    /**
     *  release the lock for file
     * @param fileName file name (path/file)
     * @param type read/write
     */
    public void release(String fileName, LockType type) {

        ReadWriteLock lock = retrieveLock(fileName);

        switch (type) {

            case READ:
                lock.readLock().unlock();
                break;
            case WRITE:
                lock.writeLock().unlock();
                break;
            default:
                // handle error scenario
                break;
        }

    }

    /**
     *  create new lock(file,read|write) or get that created before
     *  and add to lockMap(filename,read or write)
     * @param fileName file name (path/file)
     * @return read/write
     */
    private ReadWriteLock retrieveLock(String fileName) {

        ReadWriteLock newLock = null;

        try {

            registryLock.lock();

            newLock = lockMap.get(fileName);

            // create lock and add to map if it doesn't exist
            if (newLock == null) {
                newLock = new ReentrantReadWriteLock();
                lockMap.put(fileName, newLock);
            }
        } finally {

            registryLock.unlock();
        }

        return newLock;
    }

}