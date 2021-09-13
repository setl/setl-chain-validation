/* <notice>
 
    SETL Blockchain
    Copyright (C) 2021 SETL Ltd
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License, version 3, as
    published by the Free Software Foundation.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
 
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 
</notice> */
package io.setl.util;

import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.MutableInt;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePackException;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Store data for a "short time" in files. Files are never re-used and can only be deleted when all the messages in them are deleted. It is assumed that all
 * the active data can fit in memory simultaneously.
 *
 * @author Simon Greatrix on 18/07/2018.
 */
public class ShortTermStorage<Q extends MsgPackable> {

  @FunctionalInterface
  public interface Loader<Q> {

    Q load(MessageUnpacker unpacker) throws IOException;
  }



  /** A stored item. */
  static class Item<Q> {

    /** Where the item is stored. */
    Path file;

    /** The item itself. */
    Q value;
  }



  public static class STSException extends RuntimeException {

    STSException(String message, Throwable cause) {
      super(message, cause);
    }
  }



  private final Checksum checksum = new CRC32();

  /** Storage folder. */
  private final Path folder;

  private final Logger logger = LoggerFactory.getLogger(ShortTermStorage.class);

  /** When a file exceeds this size, it is closed and a new one started. */
  private final int rolloverSize;

  /** The prefix used to generate file names. */
  private final String stem;

  /** The current output file. */
  private Path currentFile;

  /** Number of entries in the current file. */
  private MutableInt currentSize;

  /** Map of active data. */
  private Map<String, Item<Q>> data = new HashMap<>();

  /** The number of files there have ever been. Used to generate new file names. */
  private long fileCount;

  /** Number of active entries in each file. */
  private NavigableMap<Path, MutableInt> fileCounts;

  /** Number of bytes written to the current file. */
  private int fileSize;

  /** The output stream. */
  private OutputStream outputStream;

  /** Output packer. */
  private MessageBufferPacker packer = MsgPackUtil.newBufferPacker();

  /** Is persistence required, or merely desired?. */
  private boolean requirePersistence = true;


  /**
   * Create a new instance.
   *
   * @param folder       where the files are stored
   * @param rolloverSize how large a single file can get in bytes before it rolls over
   * @param stem         the stem used to generate file names
   * @param loader       a function to load data
   */
  public ShortTermStorage(Path folder, int rolloverSize, String stem, Loader<Q> loader) throws IOException {
    this.folder = folder;
    this.rolloverSize = rolloverSize;
    this.stem = stem.endsWith(".") ? stem : (stem + ".");

    // Ensure folder exists.
    Files.createDirectories(folder);

    // Sort the existing files into creation order
    Comparator<Path> pathComparator = (o1, o2) -> Long.compare(numberForName(o1), numberForName(o2));

    ArrayList<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(folder, stem + "?*.sts")) {
      Iterator<Path> iter = fileStream.iterator();
      while (iter.hasNext()) {
        files.add(iter.next());
      }
      files.sort(pathComparator);
    }

    fileCounts = new TreeMap<>(pathComparator);

    // Read the files.
    for (Path file : files) {
      if (logger.isDebugEnabled()) {
        logger.debug("Loading file {}", file);
      }
      loadFile(file, loader);
    }

    // Delete empty files
    cleanFiles();

    // What number have we reached?
    if (!files.isEmpty()) {
      fileCount = numberForName(files.get(files.size() - 1));
    } else {
      fileCount = 0;
    }

    // open the new output
    newFile();
  }


  /**
   * Clean empty files. A file can be cleaned if it contains no more messages and is the oldest file. A file with no messages in it may contains deletes for
   * older files, and hence must be retained until all older files are removed.
   */
  private void cleanFiles() {
    // TODO : Compression : If there are 3 of more files, and a leading set of files is less than x% full, then re-write all the elements in that leading set.
    //
    Set<Entry<Path, MutableInt>> allFiles = fileCounts.entrySet();
    Iterator<Entry<Path, MutableInt>> iter = allFiles.iterator();
    while (iter.hasNext()) {
      Entry<Path, MutableInt> e = iter.next();

      // do not clean current file
      if (e.getKey().equals(currentFile)) {
        continue;
      }

      // clean if empty
      if (e.getValue().intValue() == 0) {
        Path path = e.getKey();
        if (logger.isDebugEnabled()) {
          logger.debug("Deleting file {}", path);
        }
        try {
          Files.delete(path);
        } catch (NoSuchFileException nsfe) {
          logger.info("Storage file {} was deleted by another process.", path);
        } catch (IOException ioe) {
          logger.error("Failed to delete storage file {}", path);
        }
        iter.remove();
      } else {
        // Future files may contain deletes even if they are empty, so stop
        return;
      }
    }
  }


  public void close() throws IOException {
    outputStream.close();
    packer.close();
  }


  public Q get(String label) {
    Item<Q> item = data.get(label);
    return item != null ? item.value : null;
  }


  public boolean isRequirePersistence() {
    return requirePersistence;
  }


  private void loadFile(Path file, Loader<Q> loader) throws IOException {
    MutableInt counter = new MutableInt(0);
    fileCounts.put(file, counter);
    try (
        InputStream input = Files.newInputStream(file);
        MessageUnpacker unpacker = MsgPackUtil.newUnpacker(input)
    ) {
      while (unpacker.hasNext()) {
        long sum;
        int length;
        byte[] payload;
        try {
          sum = unpacker.unpackLong();
          length = unpacker.unpackBinaryHeader();
          payload = unpacker.readPayload(length);
        } catch (MessageInsufficientBufferException | EOFException e) {
          // file was truncated
          logger.error("File {} was truncated. Final record could not be read. Data was lost.", file.getFileName(), e);
          return;
        }

        // verify it is a good record
        checksum.reset();
        checksum.update(payload, 0, length);
        if (sum != checksum.getValue()) {
          logger.error("Invalid checksum in file {}. Skipping record. Data lost.", file);
          continue;
        }

        // Attempt to unpack
        try (MessageUnpacker buffer = MsgPackUtil.newUnpacker(payload)) {
          boolean isMessage = buffer.unpackBoolean();
          String label = buffer.unpackString();
          if (isMessage) {
            Q q = loader.load(buffer);
            Item<Q> item = new Item<>();
            item.value = q;
            item.file = file;
            data.put(label, item);
            counter.increment();
          } else {
            Item<Q> item = data.remove(label);
            if (item != null) {
              fileCounts.get(item.file).decrement();
            }
          }
        }
      }
    } catch (MessagePackException mpe) {
      // If this happens, our message structure is broken so we cannot recover.
      logger.error("Invalid data in file {}. Cannot read any more of this file. Data may be lost.", file, mpe);
    } catch (IOException ioe) {
      logger.error("Failed to read file {}. Data may be lost.", file, ioe);
      throw ioe;
    }

  }


  private void newFile() throws IOException {
    fileCount++;
    currentFile = folder.resolve(stem + Long.toString(fileCount, 32) + ".sts");
    currentSize = new MutableInt(0);
    fileCounts.put(currentFile, currentSize);
    outputStream = Files.newOutputStream(currentFile);
    fileSize = 0;
    if (logger.isDebugEnabled()) {
      logger.debug("Starting file {}", currentFile);
    }
  }


  private long numberForName(Path f) {
    Path name = f.getFileName();
    if (name == null) {
      return -1;
    }
    String n = name.toString();
    if (n.startsWith(stem) && n.endsWith(".sts") && n.length() > 4 + stem.length()) {
      n = n.substring(stem.length(), n.length() - 4);
      try {
        return Long.parseLong(n, 32);
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }


  private void output(byte[] data) throws IOException {
    checksum.reset();
    checksum.update(data, 0, data.length);
    long sum = checksum.getValue();

    packer.clear();
    packer.packLong(sum);
    packer.packBinaryHeader(data.length);
    packer.addPayload(data);
    byte[] bytes = packer.toByteArray();

    outputStream.write(bytes);
    outputStream.flush();
    fileSize += bytes.length;

    if (fileSize < rolloverSize) {
      return;
    }

    // do roll-over
    outputStream.close();
    newFile();
  }


  /**
   * Store the message against the label.
   *
   * @param label   the message's unique label
   * @param message the message
   */
  public void put(String label, Q message) {
    if (data.containsKey(label)) {
      remove(label);
    }

    Item<Q> item = new Item<>();
    item.value = message;
    item.file = currentFile;

    MutableInt size = currentSize;
    try {
      packer.clear();
      packer.packBoolean(true);
      packer.packString(label);
      try {
        message.pack(packer);
      } catch (Exception e) {
        throw new IOException("Message packing failed for " + label, e);
      }
      packer.flush();
      output(packer.toByteArray());
    } catch (IOException ioe) {
      logger.error("Failed to write record {}", label, ioe);
      if (requirePersistence) {
        throw new STSException("Failed to write record \"" + label + "\"", ioe);
      }
    }

    size.increment();
    data.put(label, item);
  }


  /**
   * Remove the specified message from storage.
   *
   * @param label the message's label
   *
   * @return true if the label was known and removed
   */
  public boolean remove(String label) {
    try {
      packer.clear();
      packer.packBoolean(false);
      packer.packString(label);
      packer.flush();
      output(packer.toByteArray());
    } catch (IOException ioe) {
      logger.error("Failed to write delete record for {}", label, ioe);
      if (requirePersistence) {
        throw new STSException("Failed to write delete record for \"" + label + "\"", ioe);
      }
    }

    Item<Q> item = data.remove(label);
    if (item == null) {
      return false;
    }
    int newSize = fileCounts.get(item.file).decrement();
    if (newSize == 0 && !item.file.equals(currentFile)) {
      // worth checking if we can delete something
      cleanFiles();
    }
    return true;
  }


  /**
   * Is persistence required or merely desired? If persistence is required, "put" and "remove" will throw an STSException on failure to persist the change. If
   * it is not required, the store will continue to operate using its in-memory copy.
   *
   * @param requirePersistence new value
   */
  public void setRequirePersistence(boolean requirePersistence) {
    this.requirePersistence = requirePersistence;
  }


  public int size() {
    return data.size();
  }


  public Stream<Entry<String, Q>> streamEntries() {
    return data.entrySet().stream().map(entry -> new SimpleImmutableEntry<>(entry.getKey(), entry.getValue().value));
  }


  public Stream<String> streamKeys() {
    return data.keySet().stream();
  }


  public Stream<Q> streamValues() {
    return data.values().stream().map(item -> item.value);
  }
}
