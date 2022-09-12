package cuchaz.enigma.analysis;

import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import cuchaz.enigma.Enigma;

public class InterpreterPair<V extends Value, W extends Value> extends Interpreter<InterpreterPair.PairValue<V, W>> {
	private final Interpreter<V> left;
	private final Interpreter<W> right;

	public InterpreterPair(Interpreter<V> left, Interpreter<W> right) {
		super(Enigma.ASM_VERSION);
		this.left = left;
		this.right = right;
	}

	@Override
	public PairValue<V, W> newValue(Type type) {
		return pair(left.newValue(type), right.newValue(type));
	}

	@Override
	public PairValue<V, W> newOperation(AbstractInsnNode insn) throws AnalyzerException {
		return pair(left.newOperation(insn), right.newOperation(insn));
	}

	@Override
	public PairValue<V, W> copyOperation(AbstractInsnNode insn, PairValue<V, W> value) throws AnalyzerException {
		return pair(left.copyOperation(insn, value.left), right.copyOperation(insn, value.right));
	}

	@Override
	public PairValue<V, W> unaryOperation(AbstractInsnNode insn, PairValue<V, W> value) throws AnalyzerException {
		return pair(left.unaryOperation(insn, value.left), right.unaryOperation(insn, value.right));
	}

	@Override
	public PairValue<V, W> binaryOperation(AbstractInsnNode insn, PairValue<V, W> value1, PairValue<V, W> value2) throws AnalyzerException {
		return pair(left.binaryOperation(insn, value1.left, value2.left), right.binaryOperation(insn, value1.right, value2.right));
	}

	@Override
	public PairValue<V, W> ternaryOperation(AbstractInsnNode insn, PairValue<V, W> value1, PairValue<V, W> value2, PairValue<V, W> value3) throws AnalyzerException {
		return pair(left.ternaryOperation(insn, value1.left, value2.left, value3.left), right.ternaryOperation(insn, value1.right, value2.right, value3.right));
	}

	@Override
	public PairValue<V, W> naryOperation(AbstractInsnNode insn, List<? extends PairValue<V, W>> values) throws AnalyzerException {
		return pair(left.naryOperation(insn, values.stream().map(v -> v.left).toList()), right.naryOperation(insn, values.stream().map(v -> v.right).toList()));
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, PairValue<V, W> value, PairValue<V, W> expected) throws AnalyzerException {
		left.returnOperation(insn, value.left, expected.left);
		right.returnOperation(insn, value.right, expected.right);
	}

	@Override
	public PairValue<V, W> merge(PairValue<V, W> value1, PairValue<V, W> value2) {
		return pair(left.merge(value1.left, value2.left), right.merge(value1.right, value2.right));
	}

	private PairValue<V, W> pair(V left, W right) {
		if (left == null && right == null) {
			return null;
		}

		return new PairValue<>(left, right);
	}

	public static final class PairValue<V extends Value, W extends Value> implements Value {
		public final V left;
		public final W right;

		public PairValue(V left, W right) {
			if (left == null && right == null) {
				throw new IllegalArgumentException("should use null rather than pair of nulls");
			}

			this.left = left;
			this.right = right;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof InterpreterPair.PairValue pairValue && Objects.equals(left, pairValue.left) && Objects.equals(right, pairValue.right);
		}

		@Override
		public int hashCode() {
			return left.hashCode() * 31 + right.hashCode();
		}

		@Override
		public int getSize() {
			return (left == null ? right : left).getSize();
		}
	}
}
