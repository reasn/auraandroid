package io.auraapp.auraandroid.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method may be invoked by the OS at any time.
 * Such a method must wrap its entire body (except for super()
 * invocations and return values) in Handler#post()
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ExternalInvocation {
}
