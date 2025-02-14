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
import io.scif.SCIFIOPlugin;

import java.io.IOException;

import org.scijava.io.handle.DataHandle;
import org.scijava.io.location.Location;
import org.scijava.plugin.SingletonPlugin;

/**
 * This class is an interface for any kind of compression or decompression. Data
 * is presented to the compressor in a 1D or 2D byte array, with (optionally,
 * depending on the compressor) pixel dimensions and an Object containing any
 * other options the compressor may need. If an argument is not appropriate for
 * the compressor type, it is expected to completely ignore the argument. i.e.:
 * Passing a compressor that does not require pixel dimensions null for the
 * dimensions must not cause the compressor to throw a NullPointerException.
 * Classes implementing the Codec interface are expected to either implement
 * both compression methods or neither. (The same is expected for
 * decompression).
 *
 * @author Eric Kjellman
 */
public interface Codec extends SCIFIOPlugin, SingletonPlugin {

	/**
	 * Compresses a block of data.
	 *
	 * @param data The data to be compressed.
	 * @param options Options to be used during compression, if appropriate.
	 * @return The compressed data.
	 * @throws FormatException If input is not a compressed data block of the
	 *           appropriate type.
	 */
	byte[] compress(byte[] data, CodecOptions options) throws FormatException;

	/**
	 * Compresses a block of data.
	 *
	 * @param data The data to be compressed.
	 * @param options Options to be used during compression, if appropriate.
	 * @return The compressed data.
	 * @throws FormatException If input is not a compressed data block of the
	 *           appropriate type.
	 */
	byte[] compress(byte[][] data, CodecOptions options) throws FormatException;

	/**
	 * Decompresses a block of data.
	 *
	 * @param data the data to be decompressed
	 * @param options Options to be used during decompression.
	 * @return the decompressed data.
	 * @throws FormatException If data is not valid.
	 */
	byte[] decompress(byte[] data, CodecOptions options) throws FormatException;

	/**
	 * Decompresses a block of data.
	 *
	 * @param data the data to be decompressed
	 * @param options Options to be used during decompression.
	 * @return the decompressed data.
	 * @throws FormatException If data is not valid.
	 */
	byte[] decompress(byte[][] data, CodecOptions options) throws FormatException;

	/**
	 * Decompresses a block of data.
	 *
	 * @param data the data to be decompressed.
	 * @return The decompressed data.
	 * @throws FormatException If data is not valid compressed data for this
	 *           decompressor.
	 */
	byte[] decompress(byte[] data) throws FormatException;

	/**
	 * Decompresses a block of data.
	 *
	 * @param data The data to be decompressed.
	 * @return The decompressed data.
	 * @throws FormatException If data is not valid compressed data for this
	 *           decompressor.
	 */
	byte[] decompress(byte[][] data) throws FormatException;

	/**
	 * Decompresses data from the given DataHandle.
	 *
	 * @param in The stream from which to read compressed data.
	 * @param options Options to be used during decompression.
	 * @return The decompressed data.
	 * @throws FormatException If data is not valid compressed data for this
	 *           decompressor.
	 */
	byte[] decompress(DataHandle<Location> in, CodecOptions options)
		throws FormatException, IOException;

}
