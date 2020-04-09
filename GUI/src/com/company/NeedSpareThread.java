package com.company;

import java.lang.annotation.*;

/**
 * Annotation for informing {@link MultiThreadFTPClientHandler}.
 * Methods annotated with {@link NeedSpareThread} will be executed on another
 * thread. However, the result value of such annotated method isn't handled
 * by {@link MultiThreadFTPClientHandler}, which means that such method should
 * declare {@code void} as returned type.
 * <p><b>NOTE: </b>Only methods of {@link FTPClient} interface should be
 * annotated with {@link NeedSpareThread}, or else it makes no difference,
 * since {@link MultiThreadFTPClientHandler} only works with interface.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NeedSpareThread {
}
