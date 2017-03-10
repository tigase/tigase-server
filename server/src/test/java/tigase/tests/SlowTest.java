package tigase.tests;

/**
 * Interface used in annotation as a category to mark long running JUnit tests,
 * so they can be run on server when snapshot is created and not during
 * normal compilation in development environment
 * 
 * @author andrzej
 */
public interface SlowTest {}
