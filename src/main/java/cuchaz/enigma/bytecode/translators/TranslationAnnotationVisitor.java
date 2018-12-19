package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import org.objectweb.asm.AnnotationVisitor;

public class TranslationAnnotationVisitor extends AnnotationVisitor {
	private final Translator translator;
	private final ClassEntry annotationEntry;

	public TranslationAnnotationVisitor(Translator translator, ClassEntry annotationEntry, int api, AnnotationVisitor av) {
		super(api, av);
		this.translator = translator;
		this.annotationEntry = annotationEntry;
	}

	@Override
	public void visit(String name, Object value) {
		super.visit(name, translator.translateValue(value));
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return this;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		TypeDescriptor type = new TypeDescriptor(desc);
		FieldEntry annotationField = translator.translate(new FieldEntry(annotationEntry, name, type));
		return super.visitAnnotation(annotationField.getName(), annotationField.getDesc().toString());
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		TypeDescriptor type = new TypeDescriptor(desc);
		FieldEntry annotationField = translator.translate(new FieldEntry(annotationEntry, name, type));
		FieldEntry enumField = translator.translate(new FieldEntry(type.getTypeEntry(), value, type));
		super.visitEnum(annotationField.getName(), annotationField.getDesc().toString(), enumField.getName());
	}
}
