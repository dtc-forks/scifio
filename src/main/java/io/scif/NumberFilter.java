/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2023 SCIFIO developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package io.scif;

import java.io.File;
import java.io.FileFilter;
import java.math.BigInteger;

/**
 * NumberFilter is a helper filter for FilePattern.findPattern().
 */
public class NumberFilter implements FileFilter {

	// -- Fields --

	/** String appearing before the numerical block. */
	private final String pre;

	/** String appearing after the numerical block. */
	private final String post;

	// -- Constructor --

	/**
	 * Creates a filter for files containing a numerical block, sandwiched between
	 * the given strings.
	 */
	public NumberFilter(final String pre, final String post) {
		this.pre = pre;
		this.post = post;
	}

	// -- NumberFilter API methods --

	/** Gets numbers filling the asterisk positions. */
	public Integer getNumber(final String name) {
		if (!name.startsWith(pre) || !name.endsWith(post)) return null;
		final int ndx = pre.length();
		final int end = name.length() - post.length();
		try {
			return new Integer(name.substring(ndx, end));
		}
		catch (NumberFormatException | IndexOutOfBoundsException exc) {
			return null;
		}
	}

	/** Tests if a specified file should be included in a file list. */
	public boolean accept(final String name) {
		return getNumber(name) != null;
	}

	// -- FileFilter API methods --

	/** Tests if a specified file should be included in a file list. */
	@Override
	public boolean accept(final File pathname) {
		return accept(pathname.getName());
	}

}
