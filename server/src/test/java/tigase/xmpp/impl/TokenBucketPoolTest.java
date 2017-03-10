package tigase.xmpp.impl;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import tigase.TestLogger;

public class TokenBucketPoolTest {

	private static final Logger log = TestLogger.getLogger(TokenBucketPoolTest.class);

	private static final double makeTest(final TokenBucketPool t, final long testTime) throws InterruptedException {
		long registrations = 0;
		final long startTime = System.currentTimeMillis();
		long endTime;
		while ((endTime = System.currentTimeMillis()) - startTime < testTime) {
			boolean b = t.consume("default");
			if (b) {
				++registrations;
			}
			Thread.sleep(3);
		}

		log.log(Level.FINE, "Received " + registrations + " events in " + ((endTime - startTime) / 1000.0) + " seconds ("
				+ registrations / ((endTime - startTime) / 1000.0) + " eps).");

		return registrations / ((endTime - startTime) / 1000.0);
	}

	@Test
	public void testItem02() {
		TokenBucketPool.TokenBucket it = new TokenBucketPool.TokenBucket(0, 10f, TimeUnit.MILLISECONDS.toNanos(1000));
		Assert.assertEquals(1, it.getAllowance(), 0);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(1000));
		Assert.assertEquals(10, it.getAllowance(), 0);

		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertEquals(0, it.getAllowance(), 0);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(1001));
		Assert.assertEquals((float) 0.01, it.getAllowance(), 0.001);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(1002));
		Assert.assertEquals((float) 0.02, it.getAllowance(), 0.001);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(1500));
		Assert.assertEquals((float) 5, it.getAllowance(), 0.001);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(1501));
		Assert.assertEquals((float) 5.01, it.getAllowance(), 0.001);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(1503));
		Assert.assertEquals((float) 5.03, it.getAllowance(), 0.001);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(1601));
		Assert.assertEquals((float) 6.01, it.getAllowance(), 0.001);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(2000));
		Assert.assertEquals((float) 10.0, it.getAllowance(), 0.001);

		it.updateAllowance(TimeUnit.MILLISECONDS.toNanos(102000));
		Assert.assertEquals((float) 10.0, it.getAllowance(), 0.001);
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertEquals(9, it.getAllowance(), 0);
	}

	@Test
	public void testItem01() {
		TokenBucketPool.TokenBucket it = new TokenBucketPool.TokenBucket(0, 10f, 2f);
		Assert.assertEquals(1, it.getAllowance(), 0);

		it.updateAllowance(1);
		Assert.assertEquals(6, it.getAllowance(), 0);
		it.updateAllowance(1);
		Assert.assertEquals(6, it.getAllowance(), 0);

		it.updateAllowance(2);
		Assert.assertEquals(10, it.getAllowance(), 0);

		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertEquals(9, it.getAllowance(), 0);

		it.updateAllowance(3);
		Assert.assertEquals(10, it.getAllowance(), 0);

		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertEquals(1, it.getAllowance(), 0);

		it.updateAllowance(4);

		Assert.assertEquals(6, it.getAllowance(), 0);
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertTrue(it.consumeNoUpdate());
		Assert.assertFalse(it.consumeNoUpdate());
		Assert.assertEquals(0, it.getAllowance(), 0);
	}

	@Test
	public void testPurge() throws InterruptedException {
		TokenBucketPool t = new TokenBucketPool(1, 1, TimeUnit.NANOSECONDS);
		t.setAutoPurgeEnabled(false);

		t.consume("jeden");
		Assert.assertEquals(1, t.size());
		t.consume("jeden");
		Assert.assertEquals(1, t.size());

		t.consume("dwa");
		Assert.assertEquals(2, t.size());

		t.consume("trzy");
		Assert.assertEquals(3, t.size());

		Thread.sleep(3);

		Assert.assertEquals(3, t.size());
		t.purge();
		Assert.assertEquals(0, t.size());

	}

	private void assertInRange(double expectedValue, double devPercent, double value) {
		if (Math.abs(value - expectedValue) > (devPercent * expectedValue)) {
			Assert.fail("Value not in range: expected " + expectedValue + " +/- " + (devPercent * expectedValue)
					+ ", but received " + value);
		}
	}

	public void testConsume() throws Exception {
		assertInRange(2, 0.06, makeTest(new TokenBucketPool(2, 1, TimeUnit.SECONDS), TimeUnit.SECONDS.toMillis(60)));
	}

}