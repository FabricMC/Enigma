package cuchaz.enigma.api.view.index;

import java.util.Collection;

import cuchaz.enigma.api.view.entry.ClassEntryView;
import cuchaz.enigma.api.view.entry.EntryReferenceView;
import cuchaz.enigma.api.view.entry.FieldEntryView;
import cuchaz.enigma.api.view.entry.MethodEntryView;

public interface ReferenceIndexView {
	Collection<? extends MethodEntryView> getMethodsReferencedBy(MethodEntryView entry);
	Collection<? extends EntryReferenceView> getReferencesToClass(ClassEntryView entry);
	Collection<? extends EntryReferenceView> getReferencesToField(FieldEntryView entry);
	Collection<? extends EntryReferenceView> getReferencesToMethod(MethodEntryView entry);
	Collection<? extends EntryReferenceView> getFieldTypeReferencesToClass(ClassEntryView entry);
	Collection<? extends EntryReferenceView> getMethodTypeReferencesToClass(ClassEntryView entry);
}
