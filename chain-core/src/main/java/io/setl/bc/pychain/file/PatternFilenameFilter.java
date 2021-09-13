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
package io.setl.bc.pychain.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class PatternFilenameFilter implements FilenameFilter {

  private final Pattern pattern;

  /**
   * Constructs a pattern file name filter object.
   *
   * @param patternStr the pattern string on which to filter file names
   * @throws PatternSyntaxException if pattern compilation fails (runtime)
   */
  public PatternFilenameFilter(String patternStr) {
    this(Pattern.compile(patternStr));
  }

  /**
   * Constructs a pattern file name filter object.
   *
   * @param pattern the pattern on which to filter file names
   */
  public PatternFilenameFilter(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean accept(File dir, String fileName) {
    return pattern.matcher(fileName).matches();
  }
}
