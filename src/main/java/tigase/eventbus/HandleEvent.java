package tigase.eventbus;

import java.lang.annotation.*;

/**
 * Annotation to mark method as event handler. <br>
 *
 * Example:
 * 
 * <pre>
 * <code>
 	public class Consumer {
		&#64;HandleEvent
		public void onCatchSomeNiceEvent(Event01 event) {
		}
 		&#64;HandleEvent
 		public void onCatchSomeNiceEvent(Event02 event) {
 		}
	}
 * </code>
 * </pre>
 * 
 * Handler method must have only one argument with type equals to expected
 * event.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface HandleEvent {

	Type filter() default Type.all;

	enum Type {
		remote,
		local,
		all
	}
}
