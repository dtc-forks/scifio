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

package io.scif.codec;

import io.scif.FormatException;
import io.scif.UnsupportedCompressionException;

import java.io.IOException;

import org.scijava.io.handle.DataHandle;
import org.scijava.io.location.Location;
import org.scijava.plugin.Plugin;

/**
 * Methods for compressing and decompressing data using Microsoft RLE.
 */
@Plugin(type = Codec.class)
public class MSRLECodec extends AbstractCodec {

	@Override
	public byte[] compress(final byte[] data, final CodecOptions options)
		throws FormatException
	{
		throw new UnsupportedCompressionException(
			"MSRLE compression not supported.");
	}

	/**
	 * The CodecOptions parameter should have the following fields set:
	 * {@link CodecOptions#width width} {@link CodecOptions#height height}
	 * {@link CodecOptions#previousImage previousImage}
	 *
	 * @see Codec#decompress(DataHandle, CodecOptions)
	 */
	@Override
	public byte[] decompress(final DataHandle<Location> in, CodecOptions options)
		throws FormatException, IOException
	{
		if (in == null) throw new IllegalArgumentException(
			"No data to decompress.");
		if (options == null) options = CodecOptions.getDefaultOptions();

		int code = 0;
		short extra = 0;
		int stream = 0;

		int pixelPt = 0;
		int rowPt = (options.height - 1) * options.width;
		final int frameSize = options.height * options.width;

		if (options.previousImage == null) {
			options.previousImage = new byte[frameSize];
		}

		while (rowPt >= 0 && in.offset() < in.length() &&
			pixelPt < options.previousImage.length)
		{
			stream = in.read() & 0xff;
			code = stream;

			if (code == 0) {
				stream = in.read() & 0xff;
				if (stream == 0) {
					rowPt -= options.width;
					pixelPt = 0;
				}
				else if (stream == 1) return options.previousImage;
				else if (stream == 2) {
					stream = in.read() & 0xff;
					pixelPt += stream;
					stream = in.read() & 0xff;
					rowPt -= stream * options.width;
				}
				else {
					if ((rowPt + pixelPt + stream > frameSize) || (rowPt < 0)) {
						return options.previousImage;
					}

					code = stream;
					extra = (short) (stream & 0x01);
					if (stream + code + extra > in.length()) return options.previousImage;

					while (code-- > 0) {
						stream = in.read();
						options.previousImage[rowPt + pixelPt] = (byte) stream;
						pixelPt++;
					}
					if (extra != 0) in.skipBytes(1);
				}
			}
			else {
				if ((rowPt + pixelPt + stream > frameSize) || (rowPt < 0)) {
					return options.previousImage;
				}

				stream = in.read();

				while (code-- > 0) {
					options.previousImage[rowPt + pixelPt] = (byte) stream;
					pixelPt++;
				}
			}
		}

		return options.previousImage;
	}

}
