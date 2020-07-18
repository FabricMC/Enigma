package cuchaz.enigma.gui.util;

import java.awt.GridBagConstraints;

public class GridBagConstraintsBuilder {

	private final GridBagConstraints inner;

	private GridBagConstraintsBuilder(GridBagConstraints inner) {
		this.inner = inner;
	}

	public static GridBagConstraintsBuilder create() {
		return new GridBagConstraintsBuilder(new GridBagConstraints());
	}

	public GridBagConstraintsBuilder pos(int x, int y) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.gridx = x;
		copy.inner.gridy = y;
		return copy;
	}

	public GridBagConstraintsBuilder size(int width, int height) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.gridwidth = width;
		copy.inner.gridheight = height;
		return copy;
	}

	public GridBagConstraintsBuilder width(int width) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.gridwidth = width;
		return copy;
	}

	public GridBagConstraintsBuilder height(int height) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.gridheight = height;
		return copy;
	}

	public GridBagConstraintsBuilder dimensions(int x, int y, int width, int height) {
		return this.pos(x, y).size(width, height);
	}

	public GridBagConstraintsBuilder weight(double x, double y) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.weightx = x;
		copy.inner.weighty = y;
		return copy;
	}

	public GridBagConstraintsBuilder weightX(double x) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.weightx = x;
		return copy;
	}

	public GridBagConstraintsBuilder weightY(double y) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.weighty = y;
		return copy;
	}

	public GridBagConstraintsBuilder anchor(int anchor) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.anchor = anchor;
		return copy;
	}

	public GridBagConstraintsBuilder fill(int fill) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.fill = fill;
		return copy;
	}

	public GridBagConstraintsBuilder insets(int all) {
		return this.insets(all, all, all, all);
	}

	public GridBagConstraintsBuilder insets(int vertical, int horizontal) {
		return this.insets(vertical, horizontal, vertical, horizontal);
	}

	public GridBagConstraintsBuilder insets(int top, int horizontal, int bottom) {
		return this.insets(top, horizontal, bottom, horizontal);
	}

	public GridBagConstraintsBuilder insets(int top, int right, int bottom, int left) {
		GridBagConstraintsBuilder copy = this.copy();
		copy.inner.insets.set(top, left, bottom, right);
		return copy;
	}

	public GridBagConstraintsBuilder copy() {
		GridBagConstraints c = (GridBagConstraints) this.inner.clone();
		return new GridBagConstraintsBuilder(c);
	}

	public GridBagConstraints build() {
		return (GridBagConstraints) this.inner.clone();
	}

}
