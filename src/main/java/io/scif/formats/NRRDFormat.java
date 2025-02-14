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

package io.scif.formats;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.MetadataLevel;
import io.scif.UnsupportedCompressionException;
import io.scif.config.SCIFIOConfig;
import io.scif.services.FormatService;
import io.scif.util.FormatTools;

import java.io.File;
import java.io.IOException;

import net.imagej.axis.Axes;
import net.imglib2.Interval;

import org.scijava.io.handle.DataHandle;
import org.scijava.io.handle.DataHandleService;
import org.scijava.io.location.BrowsableLocation;
import org.scijava.io.location.Location;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * File format reader for NRRD files; see http://teem.sourceforge.net/nrrd.
 */
@Plugin(type = Format.class, name = "NRRD")
public class NRRDFormat extends AbstractFormat {

	// -- AbstractFormat Methods --

	@Override
	protected String[] makeSuffixArray() {
		return new String[] { "nrrd", "nhdr" };
	}

	// -- Nested classes --

	public static class Metadata extends AbstractMetadata {

		// -- Fields --

		/** Location of data file, if the current extension is 'nhdr'. */
		private Location dataFile;

		/** Data encoding. */
		private String encoding;

		/** Offset to pixel data. */
		private long offset;

		/** Helper format for reading pixel data. */
		private io.scif.Reader helper;

		private String[] pixelSizes;

		private boolean lookForCompanion = true;

		private boolean initializeHelper = false;

		// -- NRRDMetadata getters and setters --

		public void setHelper(final io.scif.Reader reader) {
			helper = reader;
		}

		public io.scif.Reader getHelper() {
			return helper;
		}

		public Location getDataFile() {
			return dataFile;
		}

		public void setDataFile(final Location dataFile) {
			this.dataFile = dataFile;
		}

		public String getEncoding() {
			return encoding;
		}

		public void setEncoding(final String encoding) {
			this.encoding = encoding;
		}

		public long getOffset() {
			return offset;
		}

		public void setOffset(final long offset) {
			this.offset = offset;
		}

		public String[] getPixelSizes() {
			return pixelSizes;
		}

		public void setPixelSizes(final String[] pixelSizes) {
			this.pixelSizes = pixelSizes;
		}

		public boolean isLookForCompanion() {
			return lookForCompanion;
		}

		public void setLookForCompanion(final boolean lookForCompanion) {
			this.lookForCompanion = lookForCompanion;
		}

		public boolean isInitializeHelper() {
			return initializeHelper;
		}

		public void setInitializeHelper(final boolean initializeHelper) {
			this.initializeHelper = initializeHelper;
		}

		// -- Metadata API methods --

		@Override
		public void populateImageMetadata() {
			final ImageMetadata iMeta = get(0);

			if (iMeta.getAxisLength(Axes.CHANNEL) > 1) {
				iMeta.setAxisTypes(Axes.CHANNEL, Axes.X, Axes.Y);
				iMeta.setPlanarAxisCount(3);
			}
			iMeta.setIndexed(false);
			iMeta.setFalseColor(false);
			iMeta.setMetadataComplete(true);
		}

		@Override
		public void close(final boolean fileOnly) throws IOException {
			super.close(fileOnly);
			if (!fileOnly) {
				dataFile = null;
				encoding = null;
				offset = 0;
				pixelSizes = null;
				initializeHelper = false;
				helper = null;
			}
		}
	}

	public static class Checker extends AbstractChecker {

		// -- Constants --

		public static final String NRRD_MAGIC_STRING = "NRRD";

		@Parameter
		private DataHandleService dataHandleService;

		// -- Checker API Methods --

		@Override
		public boolean isFormat(final Location loc, final SCIFIOConfig config) {
			if (super.isFormat(loc, config)) return true;
			if (!config.checkerIsOpen()) return false;

			if (!(loc instanceof BrowsableLocation)) return false;
			final BrowsableLocation bLoc = (BrowsableLocation) loc;
			try {
				// look for a matching .nhdr file
				String name = loc.getName();
				Location header = bLoc.sibling(name + ".nhdr");
				if (dataHandleService.exists(header)) {
					return true;
				}

				// strip current extension and try appending '.nhdr'
				if (name.contains(".")) {
					name = name.substring(0, name.lastIndexOf('.'));
				}

				header = bLoc.sibling(name + ".nhdr");
				return dataHandleService.exists(header);
			}
			catch (final IOException e) {
				return false;
			}
		}

		@Override
		public boolean isFormat(final DataHandle<Location> stream)
			throws IOException
		{
			final int blockLen = NRRD_MAGIC_STRING.length();
			if (!FormatTools.validStream(stream, blockLen, false)) return false;
			return stream.readString(blockLen).startsWith(NRRD_MAGIC_STRING);
		}
	}

	public static class Parser extends AbstractParser<Metadata> {

		@Parameter
		private FormatService formatService;

		@Parameter
		private DataHandleService dataHandleService;

		// -- Parser API Methods --

		@Override
		public Location[] getImageUsedFiles(final int imageIndex,
			final boolean noPixels)
		{
			FormatTools.assertId(getSource(), true, 1);
			if (noPixels) {
				if (getMetadata().getDataFile() == null) return null;
				return new Location[] { getSource().get() };
			}
			if (getMetadata().getDataFile() == null) return new Location[] {
				getSource().get() };
			return new Location[] { getSource().get(), getMetadata().getDataFile() };
		}
		
		// -- Abstract Parser API Methods --

		@Override
		public Metadata parse(final DataHandle<Location> stream,
			final Metadata meta, final SCIFIOConfig config) throws IOException, FormatException
		{
			final BrowsableLocation loc = asBrowsableLocation(stream);
			String id = loc.getName();
			boolean changedStream = false;
			Location newLocation = null;

			// make sure we actually have the .nrrd/.nhdr file
			if (!FormatTools.checkSuffix(id, "nhdr") && !FormatTools.checkSuffix(id,
				"nrrd"))
			{
				changedStream = true;
				id += ".nhdr";

				if (!dataHandleService.exists(loc.sibling(id))) {
					id = id.substring(0, id.lastIndexOf('.'));
					id = id.substring(0, id.lastIndexOf('.'));
					id += ".nhdr";
				}
				newLocation = loc.sibling(id);
			}

			if (changedStream) {
				stream.close();
				return super.parse(dataHandleService.create(newLocation), meta, config);
			}

			return super.parse(stream, meta, config);
		}

		@Override
		protected void typedParse(final DataHandle<Location> stream,
			final Metadata meta, final SCIFIOConfig config) throws IOException,
			FormatException
		{
			String key, v;

			int numDimensions = 0;

			meta.createImageMetadata(1);
			final ImageMetadata iMeta = meta.get(0);

			iMeta.setAxisLength(Axes.X, 1);
			iMeta.setAxisLength(Axes.Y, 1);
			iMeta.setAxisLength(Axes.Z, 1);
			iMeta.setAxisLength(Axes.CHANNEL, 1);
			iMeta.setAxisLength(Axes.TIME, 1);
			iMeta.setPlanarAxisCount(2);

			String line = getSource().readLine();
			while (line != null && line.length() > 0) {
				if (!line.startsWith("#") && !line.startsWith("NRRD")) {
					// parse key/value pair
					key = line.substring(0, line.indexOf(":")).trim();
					v = line.substring(line.indexOf(":") + 1).trim();
					meta.getTable().put(key, v);

					if (key.equals("type")) {
						if (v.contains("char") || v.contains("8")) {
							iMeta.setPixelType(FormatTools.UINT8);
						}
						else if (v.contains("short") || v.contains("16")) {
							iMeta.setPixelType(FormatTools.UINT16);
						}
						else if (v.equals("int") || v.equals("signed int") || v.equals(
							"int32") || v.equals("int32_t") || v.equals("uint") || v.equals(
								"unsigned int") || v.equals("uint32") || v.equals("uint32_t"))
						{
							iMeta.setPixelType(FormatTools.UINT32);
						}
						else if (v.equals("float")) iMeta.setPixelType(FormatTools.FLOAT);
						else if (v.equals("double")) iMeta.setPixelType(FormatTools.DOUBLE);
						else throw new FormatException("Unsupported data type: " + v);
					}
					else if (key.equals("dimension")) {
						numDimensions = Integer.parseInt(v);
					}
					else if (key.equals("sizes")) {
						final String[] tokens = v.split(" ");
						for (int i = 0; i < numDimensions; i++) {
							final int size = Integer.parseInt(tokens[i]);

							if (numDimensions >= 3 && i == 0 && size > 1 && size <= 16) {
								iMeta.setAxisLength(Axes.CHANNEL, size);
								iMeta.setPlanarAxisCount(3);
							}
							else if (i == 0 || (iMeta.getPlanarAxisCount() > 2 && i == 1)) {
								iMeta.setAxisLength(Axes.X, size);
							}
							else if (i == 1 || (iMeta.getPlanarAxisCount() > 2 && i == 2)) {
								iMeta.setAxisLength(Axes.Y, size);
							}
							else if (i == 2 || (iMeta.getPlanarAxisCount() > 2 && i == 3)) {
								iMeta.setAxisLength(Axes.Z, size);
							}
							else if (i == 3 || (iMeta.getPlanarAxisCount() > 2 && i == 4)) {
								iMeta.setAxisLength(Axes.TIME, size);
							}
						}
					}
					else if (key.equals("data file") || key.equals("datafile")) {
						meta.setDataFile(asBrowsableLocation(stream).sibling(v));
					}
					else if (key.equals("encoding")) meta.setEncoding(v);
					else if (key.equals("endian")) {
						iMeta.setLittleEndian(v.equals("little"));
					}
					else if (key.equals("spacings")) {
						meta.setPixelSizes(v.split(" "));
					}
					else if (key.equals("byte skip")) {
						meta.setOffset(Long.parseLong(v));
					}
				}

				line = getSource().readLine();
				if (line != null) line = line.trim();
			}

			// nrrd files store pixel data in addition to metadata
			// nhdr files don't store pixel data, but instead provide a path to
			// the pixels file (this can be any format)

			if (meta.getDataFile() == null) meta.setOffset(stream.offset());
			else {
				final BrowsableLocation f = asBrowsableLocation(getSource());
				final Location parent = f.parent();
				if (dataHandleService.exists(f) && parent != null) {
					String dataFile = meta.getDataFile().getName();
					dataFile = dataFile.substring(dataFile.indexOf(File.separator) + 1);
					final Location dataLocation = f.sibling(dataFile);
					meta.setDataFile(dataLocation);
				}
				meta.setInitializeHelper(!meta.getEncoding().equals("raw"));
			}

			if (meta.isInitializeHelper()) {
				// Find the highest priority non-NRRD format that can support
				// the current image and cache it as a helper
				final NRRDFormat nrrd = formatService.getFormatFromClass(
					NRRDFormat.class);
				formatService.removeFormat(nrrd);

				final Format helperFormat = formatService.getFormat(meta.getDataFile(),
					config);
				final io.scif.Parser p = helperFormat.createParser();
				final io.scif.Reader helper = helperFormat.createReader();
				helper.setMetadata(p.parse(meta.getDataFile(), new SCIFIOConfig(getContext())
					.parserSetLevel(MetadataLevel.MINIMUM)));
				helper.setSource(meta.getDataFile(), config);
				meta.setHelper(helper);

				formatService.addFormat(nrrd);
			}
		}

		// -- Groupable API Methods --

		@Override
		public boolean hasCompanionFiles() {
			return true;
		}

		@Override
		public boolean isSingleFile(final Location id) throws FormatException,
			IOException
		{
			return FormatTools.checkSuffix(id.getName(), "nrrd");
		}

		@Override
		public int fileGroupOption(final Location id) throws FormatException,
			IOException
		{
			return FormatTools.MUST_GROUP;
		}
	}

	public static class Reader extends ByteArrayReader<Metadata> {

		// -- AbstractReader API Methods --

		@Parameter
		private DataHandleService dataHandleService;

		@Override
		protected String[] createDomainArray() {
			return new String[] { FormatTools.UNKNOWN_DOMAIN };
		}

		// -- Groupable API Methods --

		@Override
		public boolean hasCompanionFiles() {
			return true;
		}

		@Override
		public boolean isSingleFile(final Location id) throws FormatException,
			IOException
		{
			return FormatTools.checkSuffix(id.getName(), "nrrd");
		}

		@Override
		public int fileGroupOption(final Location id) throws FormatException,
			IOException
		{
			return FormatTools.MUST_GROUP;
		}

		// -- Reader API Methods --

		@Override
		public long getOptimalTileHeight(final int imageIndex) {
			return getMetadata().get(imageIndex).getAxisLength(Axes.Y);
		}

		@Override
		public ByteArrayPlane openPlane(final int imageIndex, final long planeIndex,
			final ByteArrayPlane plane, final Interval bounds,
			final SCIFIOConfig config) throws FormatException, IOException
		{
			final byte[] buf = plane.getData();
			final Metadata meta = getMetadata();

			FormatTools.checkPlaneForReading(meta, imageIndex, planeIndex, buf.length,
				bounds);

			// TODO : add support for additional encoding types
			if (meta.getDataFile() == null) {
				if (meta.getEncoding().equals("raw")) {
					final long planeSize = FormatTools.getPlaneSize(this, imageIndex);
					getHandle().seek(meta.getOffset() + planeIndex * planeSize);

					readPlane(getHandle(), imageIndex, bounds, plane);
					return plane;
				}
				throw new UnsupportedCompressionException("Unsupported encoding: " +
					meta.getEncoding());
			}
			else if (meta.getEncoding().equals("raw")) {
				final DataHandle<Location> s = dataHandleService.create(meta
					.getDataFile());
				s.seek(meta.getOffset() + planeIndex * FormatTools.getPlaneSize(this,
					imageIndex));
				readPlane(s, imageIndex, bounds, plane);
				s.close();
				return plane;
			}

			// open the data file using our helper format
			if (meta.isInitializeHelper() && meta.getDataFile() != null && meta
				.getHelper() != null)
			{
				meta.getHelper().openPlane(imageIndex, planeIndex, plane, bounds,
					config);
				return plane;
			}

			throw new FormatException("Could not find a supporting Format");
		}

	}
}
