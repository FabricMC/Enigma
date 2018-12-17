package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.FieldDefEntry;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;

public class TranslationFieldVisitor extends FieldVisitor {
	private final FieldDefEntry fieldEntry;
	private final Translator translator;

	public TranslationFieldVisitor(Translator translator, FieldDefEntry fieldEntry, int api, FieldVisitor fv) {
		super(api, fv);
		this.translator = translator;
		this.fieldEntry = fieldEntry;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		TypeDescriptor typeDesc = translator.getTranslatedTypeDesc(new TypeDescriptor(desc));
		AnnotationVisitor av = super.visitAnnotation(typeDesc.toString(), visible);
		return new TranslationAnnotationVisitor(translator, typeDesc.getTypeEntry(), api, av);
	}
}
