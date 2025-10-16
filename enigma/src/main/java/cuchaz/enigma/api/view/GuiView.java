package cuchaz.enigma.api.view;

import javax.swing.JEditorPane;
import javax.swing.JFrame;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.api.view.entry.EntryReferenceView;
import cuchaz.enigma.api.view.entry.EntryView;

@ApiStatus.NonExtendable
public interface GuiView {
	@Nullable
	ProjectView getProject();

	@Nullable
	EntryReferenceView getCursorReference();

	@Nullable
	EntryView getCursorDeclaration();

	JFrame getFrame();

	float getScale();

	boolean isDarkTheme();

	JEditorPane createEditorPane();
}
