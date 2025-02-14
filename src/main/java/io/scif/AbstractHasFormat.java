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

import io.scif.services.FormatService;

import org.scijava.io.handle.DataHandle;
import org.scijava.io.location.BrowsableLocation;
import org.scijava.io.location.Location;
import org.scijava.plugin.Parameter;

/**
 * Abstract superclass for all classes that implement {@link io.scif.HasFormat}.
 *
 * @see io.scif.HasFormat
 * @see io.scif.services.FormatService
 * @author Mark Hiner
 */
public abstract class AbstractHasFormat extends AbstractSCIFIOPlugin implements
	HasFormat
{

	private static final String NO_FORMAT =
		"Format for this metadata could not be determined.";

	@Parameter
	private FormatService formatService;

	// -- HasFormat API --

	@Override
	public Format getFormat() {
		// All Format lookups go through the FormatService
		return formatService.getFormatFromComponent(getClass());
	}

	@Override
	public String getFormatName() {
		final Format format = getFormat();
		if (format == null) return NO_FORMAT;
		return format.getFormatName();
	}

	/**
	 * Safely casts a {@link Location} to a {@link BrowsableLocation}, throwing a
	 * {@link FormatException} if the cast is not possible.
	 *
	 * @param loc the location to cast to {@link BrowsableLocation}
	 * @throws FormatException
	 */
	protected BrowsableLocation asBrowsableLocation(final Location loc)
		throws FormatException
	{
		if (loc instanceof BrowsableLocation) {
			return (BrowsableLocation) loc;
		}
		throw new FormatException("The format: '" + getFormatName() +
			"' requires a browsable Location!");
	}

	/**
	 * Convenience overload of {@link #asBrowsableLocation(Location)} that
	 * operates on a handle.
	 *
	 * @param handle
	 * @throws FormatException
	 */
	protected BrowsableLocation asBrowsableLocation(
		final DataHandle<Location> handle) throws FormatException
	{
		return asBrowsableLocation(handle.get());
	}
}
