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
package io.setl.bc.pychain.state.test;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.DefaultProcessor;
import io.setl.bc.pychain.tx.TransactionProcessor;
import io.setl.json.jackson.CanonicalFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Simon Greatrix on 2020-01-07.
 */
public class TestBlockSimulator {


  /**
   * Take a state, simulate applying a block, and return the new state.
   *
   * @param initialState the state to start from
   * @param block        the block to apply
   *
   * @return the new state
   */
  public static State apply(State initialState, Block block) throws IOException, StateSnapshotCorruptedException {
    StateSnapshot stateSnapshot = initialState.createSnapshot();

    TransactionProcessor transactionProcessor = new DefaultProcessor();
    Txi[] tx = block.getTransactions();
    transactionProcessor.processTransactions(stateSnapshot, tx, block.getTimeStamp(), true);
    transactionProcessor.postProcessTransactions(stateSnapshot, block, block.getTimeStamp());

    transactionProcessor.removeProcessedTimeEvents(stateSnapshot, block, block.getTimeStamp());

    stateSnapshot.commit();
    return stateSnapshot.finalizeBlock(block);
  }


  /**
   * Load a BlockBuilder from a stream. UTF-8 encoding is assumed.
   *
   * @param input the stream
   *
   * @return the state
   */
  public static BlockBuilder load(InputStream input) throws IOException {
    return load(new InputStreamReader(input, UTF_8));
  }


  /**
   * Load a BlockBuilder from a file.
   *
   * @param path the file's path
   *
   * @return the state
   */
  public static BlockBuilder load(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path, UTF_8)) {
      return load(reader);
    }
  }


  /**
   * Load a BlockBuilder.
   *
   * @param reader the source of the state
   *
   * @return the state
   */
  public static BlockBuilder load(Reader reader) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(reader, BlockBuilder.class);
  }


  /**
   * Load a BlockBuilder from a class-path resource.
   *
   * @param resourceBase the class that defines the resource base (may be null, if path is absolute)
   * @param resourcePath the path relative to the class.
   *
   * @return the state
   */
  public static BlockBuilder load(Class<?> resourceBase, String resourcePath) throws IOException {
    try (InputStream input = resourceInput(resourceBase, resourcePath)) {
      return load(input);
    }
  }


  /**
   * Open an input stream from the specified resource.
   *
   * @param resourceBase the class that provides the relative root for the resource
   * @param resourcePath the path for the resource
   *
   * @return the stream
   */
  private static InputStream resourceInput(Class<?> resourceBase, String resourcePath) {
    if (resourceBase != null) {
      return resourceBase.getResourceAsStream(resourcePath);
    }
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
  }


  /**
   * Save a BlockBuilder to a JSON string.
   *
   * @param block    the block builder
   * @param isPretty if true, use pretty printing
   *
   * @return the JSON encoding of the string
   */
  public static String save(BlockBuilder block, boolean isPretty) throws IOException {
    StringWriter w = new StringWriter();
    save(w, block, isPretty);
    return w.toString();
  }


  /**
   * Write out a block builder.
   *
   * @param writer   the writer to write to
   * @param block    the block builder
   * @param isPretty if true, use pretty printing
   */
  public static void save(Writer writer, BlockBuilder block, boolean isPretty) throws IOException {
    ObjectWriter objectWriter;
    if (isPretty) {
      objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
    } else {
      objectWriter = new ObjectMapper(new CanonicalFactory()).writer();
    }
    objectWriter.writeValue(writer, block);
  }


  /**
   * Save a BlockBuilder to a file.
   *
   * @param path     the file's path
   * @param block    the block builder
   * @param isPretty if true, use pretty printing
   */
  public static void save(Path path, BlockBuilder block, boolean isPretty) throws IOException {
    try (Writer w = Files.newBufferedWriter(path, UTF_8)) {
      save(w, block, isPretty);
    }
  }
}
