package cuchaz.enigma.api.view;

import javax.swing.JFrame;

import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.api.view.entry.EntryReferenceView;

public interface GuiView {
	@Nullable
	ProjectView getProject();

	@Nullable
	EntryReferenceView getCursorReference();

	JFrame getFrame();
}
