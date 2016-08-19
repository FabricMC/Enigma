/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import com.strobel.decompiler.languages.Region;

public class Token implements Comparable<Token> {

    public int start;
    public int end;
    public int offsetLine;
    public int offsetColum;
    public String text;
    private Region region;
    private SourceIndex index;

    public Token(SourceIndex index, Region region, String source)
    {
        this(index.toPos(region.getBeginLine(), region.getBeginColumn()), index
                .toPos(region.getEndLine(), region.getEndColumn()), source);
        this.region = region;
        this.index = index;
        //toPos(region.getBeginLine(), region.getBeginColumn()), toPos(region.getEndLine(), region.getEndColumn())
    }

    public void computePos()
    {
        if (region == null)
            return;
        this.start = index.toPos(region.getBeginLine() + offsetLine, region.getBeginColumn() + offsetColum);
        this.end = index.toPos(region.getEndLine() + offsetLine, region.getEndColumn() + offsetColum);
        this.offsetLine = 0;
        this.offsetColum = 0;
        this.region = null;
    }

    public Region getRegion()
    {
        return region;
    }

    public Token(int start, int end, String source) {
        this.start = start;
        this.end = end;
        if (source != null)
            this.text = source.substring(start, end);
    }

    public boolean contains(int pos) {
        return pos >= start && pos <= end;
    }

    @Override
    public int compareTo(Token other) {
        return start - other.start;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Token && equals((Token) other);
    }

    public boolean equals(Token other) {
        return start == other.start && end == other.end;
    }

    @Override
    public String toString() {
        return String.format("[%d,%d]", start, end);
    }
}
