package de.caluga.test.mongo.suite;

import de.caluga.morphium.MorphiumSingleton;
import de.caluga.morphium.async.AsyncOperationCallback;
import de.caluga.morphium.async.AsyncOperationType;
import de.caluga.morphium.query.Query;
import org.junit.Test;

import java.util.List;

/**
 * User: Stephan Bösebeck
 * Date: 11.03.13
 * Time: 13:05
 * <p/>
 * TODO: Add documentation here
 */
@SuppressWarnings("AssertWithSideEffects")
public class AsyncOperationTest extends MongoTest {
    private boolean asyncCall = false;

    @Test
    public void asyncStoreTest() throws Exception {
        asyncCall = false;
        super.createCachedObjects(1000);
        waitForWrites();
        log.info("Uncached object preparation");
        super.createUncachedObjects(1000);
        waitForWrites();
        Query<UncachedObject> uc = MorphiumSingleton.get().createQueryFor(UncachedObject.class);
        uc = uc.f("counter").lt(100);
        MorphiumSingleton.get().delete(uc, new AsyncOperationCallback<Query<UncachedObject>>() {
            @Override
            public void onOperationSucceeded(AsyncOperationType type, Query<Query<UncachedObject>> q, long duration, List<Query<UncachedObject>> result, Query<UncachedObject> entity, Object... param) {
                log.info("Objects deleted");
            }

            @Override
            public void onOperationError(AsyncOperationType type, Query<Query<UncachedObject>> q, long duration, String error, Throwable t, Query<UncachedObject> entity, Object... param) {
                assert false;
            }
        });

        uc = uc.q();
        uc.f("counter").mod(3, 2);
        MorphiumSingleton.get().set(uc, "counter", 0, false, true, new AsyncOperationCallback<UncachedObject>() {
            @Override
            public void onOperationSucceeded(AsyncOperationType type, Query<UncachedObject> q, long duration, List<UncachedObject> result, UncachedObject entity, Object... param) {
                log.info("Objects updated");
                asyncCall = true;

            }

            @Override
            public void onOperationError(AsyncOperationType type, Query<UncachedObject> q, long duration, String error, Throwable t, UncachedObject entity, Object... param) {
                log.info("Objects update error");
            }
        });

        waitForWrites();

        assert MorphiumSingleton.get().createQueryFor(UncachedObject.class).f("counter").eq(0).countAll() > 0;
        assert (asyncCall);
    }


    @Test
    public void asyncReadTest() throws Exception {
        asyncCall = false;
        createUncachedObjects(100);
        Query<UncachedObject> q = MorphiumSingleton.get().createQueryFor(UncachedObject.class);
        q = q.f("counter").lt(1000);
        q.asList(new AsyncOperationCallback<UncachedObject>() {
            @Override
            public void onOperationSucceeded(AsyncOperationType type, Query<UncachedObject> q, long duration, List<UncachedObject> result, UncachedObject entity, Object... param) {
                log.info("got read answer");
                assert (result != null) : "Error";
                assert (result.size() == 100) : "Error";
                asyncCall = true;
            }

            @Override
            public void onOperationError(AsyncOperationType type, Query<UncachedObject> q, long duration, String error, Throwable t, UncachedObject entity, Object... param) {
                assert false;
            }
        });
        waitForAsyncOperationToStart(1000000);
        int count = 0;
        while (q.getNumberOfPendingRequests() > 0) {
            count++;
            assert (count < 10);
            System.out.println("Still waiting...");
            Thread.sleep(1000);
        }
        assert (asyncCall);
    }

    @Test
    public void asyncCountTest() throws Exception {
        asyncCall = false;
        createUncachedObjects(100);
        Query<UncachedObject> q = MorphiumSingleton.get().createQueryFor(UncachedObject.class);
        q = q.f("counter").lt(1000);
        q.countAll(new AsyncOperationCallback<UncachedObject>() {
            @Override
            public void onOperationSucceeded(AsyncOperationType type, Query<UncachedObject> q, long duration, List<UncachedObject> result, UncachedObject entity, Object... param) {
                assert (param != null && param[0] != null);
                assert (param[0].equals(Long.valueOf(100)));
                asyncCall = true;
            }

            @Override
            public void onOperationError(AsyncOperationType type, Query<UncachedObject> q, long duration, String error, Throwable t, UncachedObject entity, Object... param) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        //waiting for thread to become active
        waitForAsyncOperationToStart(1000000);
        int count = 0;
        while (q.getNumberOfPendingRequests() > 0) {
            count++;
            assert (count < 10);
            Thread.sleep(1000);
        }
        assert (asyncCall);
    }
}