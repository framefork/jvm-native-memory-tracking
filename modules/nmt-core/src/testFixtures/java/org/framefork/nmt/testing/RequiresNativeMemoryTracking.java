package org.framefork.nmt.testing;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks tests that need a JVM started with {@code -XX:NativeMemoryTracking=summary}.
 *
 * <p>NMT cannot be turned on at runtime, so the build runs these tests in the separate {@code nmtTest} task,
 * which starts the test JVM with the flag. The regular {@code test} task excludes them and runs with NMT off.</p>
 */
@Tag(RequiresNativeMemoryTracking.TAG)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresNativeMemoryTracking
{

    String TAG = "nmt";

}
