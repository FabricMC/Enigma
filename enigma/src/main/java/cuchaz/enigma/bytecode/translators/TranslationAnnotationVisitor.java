package cuchaz.enigma.bytecode.translators;

import org.objectweb.asm.AnnotationVisitor;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;

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
		super.visit(name, AsmObjectTranslator.translateValue(translator, value));
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return new TranslationAnnotationVisitor(translator, annotationEntry, api, super.visitArray(name));
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		TypeDescriptor type = new TypeDescriptor(desc);

		if (name != null) {
			FieldEntry annotationField = translator.translate(new FieldEntry(annotationEntry, name, type));
			return super.visitAnnotation(annotationField.getName(), annotationField.getDesc().toString());
		} else {
			return super.visitAnnotation(null, translator.translate(type).toString());
		}
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		TypeDescriptor type = new TypeDescriptor(desc);
		FieldEntry enumField = translator.translate(new FieldEntry(type.getTypeEntry(), value, type));

		if (name != null) {
			FieldEntry annotationField = translator.translate(new FieldEntry(annotationEntry, name, type));
			super.visitEnum(annotationField.getName(), annotationField.getDesc().toString(), enumField.getName());
		} else {
			super.visitEnum(null, translator.translate(type).toString(), enumField.getName());
		}
	}
}
