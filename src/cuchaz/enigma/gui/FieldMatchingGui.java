package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.convert.ClassMatches;
import cuchaz.enigma.convert.FieldMatches;
import cuchaz.enigma.gui.ClassSelector.ClassSelectionListener;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.FieldMapping;
import de.sciss.syntaxpane.DefaultSyntaxKit;


public class FieldMatchingGui {
	
	public static interface SaveListener {
		public void save(FieldMatches matches);
	}
	
	// controls
	private JFrame m_frame;
	private ClassSelector m_sourceClasses;
	private CodeReader m_sourceReader;
	private CodeReader m_destReader;

	private ClassMatches m_classMatches;
	private FieldMatches m_fieldMatches;
	private Map<FieldEntry,FieldMapping> m_droppedFieldMappings;
	private Deobfuscator m_sourceDeobfuscator;
	private Deobfuscator m_destDeobfuscator;
	private SaveListener m_saveListener;

	public FieldMatchingGui(ClassMatches classMatches, FieldMatches fieldMatches, Map<FieldEntry,FieldMapping> droppedFieldMappings, Deobfuscator sourceDeobfuscator, Deobfuscator destDeobfuscator) {
		
		m_classMatches = classMatches;
		m_fieldMatches = fieldMatches;
		m_droppedFieldMappings = droppedFieldMappings;
		m_sourceDeobfuscator = sourceDeobfuscator;
		m_destDeobfuscator = destDeobfuscator;
		
		// init frame
		m_frame = new JFrame(Constants.Name + " - Field Matcher");
		final Container pane = m_frame.getContentPane();
		pane.setLayout(new BorderLayout());
		
		// init classes side
		JPanel classesPanel = new JPanel();
		classesPanel.setLayout(new BoxLayout(classesPanel, BoxLayout.PAGE_AXIS));
		classesPanel.setPreferredSize(new Dimension(200, 0));
		pane.add(classesPanel, BorderLayout.WEST);
		classesPanel.add(new JLabel("Classes"));
		
		m_sourceClasses = new ClassSelector(ClassSelector.DeobfuscatedClassEntryComparator);
		m_sourceClasses.setListener(new ClassSelectionListener() {
			@Override
			public void onSelectClass(ClassEntry classEntry) {
				setSourceClass(classEntry);
			}
		});
		JScrollPane sourceScroller = new JScrollPane(m_sourceClasses);
		classesPanel.add(sourceScroller);

		// init fields side
		JPanel fieldsPanel = new JPanel();
		fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.PAGE_AXIS));
		fieldsPanel.setPreferredSize(new Dimension(200, 0));
		pane.add(fieldsPanel, BorderLayout.WEST);
		fieldsPanel.add(new JLabel("Destination Fields"));
		
		// init readers
		DefaultSyntaxKit.initKit();
		m_sourceReader = new CodeReader();
		m_destReader = new CodeReader();

		// init all the splits
		JSplitPane splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, classesPanel, new JScrollPane(m_sourceReader));
		splitLeft.setResizeWeight(0); // let the right side take all the slack
		JSplitPane splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(m_destReader), fieldsPanel);
		splitRight.setResizeWeight(1); // let the left side take all the slack
		JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, splitLeft, splitRight);
		splitCenter.setResizeWeight(0.5); // resize 50:50
		pane.add(splitCenter, BorderLayout.CENTER);
		splitCenter.resetToPreferredSizes();
		
		// init bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());

		// show the frame
		pane.doLayout();
		m_frame.setSize(1024, 576);
		m_frame.setMinimumSize(new Dimension(640, 480));
		m_frame.setVisible(true);
		m_frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// init state
		m_saveListener = null;
		m_fieldMatches.addUnmatchedSourceFields(droppedFieldMappings.keySet());
		m_sourceClasses.setClasses(m_fieldMatches.getSourceClassesWithUnmatchedFields());
	}

	public void setSaveListener(SaveListener val) {
		m_saveListener = val;
	}
	
	protected void setSourceClass(ClassEntry obfSourceClass) {
		
		// get the matched dest class
		final ClassEntry obfDestClass = m_classMatches.getUniqueMatches().get(obfSourceClass);
		if (obfDestClass == null) {
			throw new Error("No matching dest class for source class: " + obfSourceClass);
		}

		m_sourceReader.decompileClass(obfSourceClass, m_sourceDeobfuscator);
		m_destReader.decompileClass(obfDestClass, m_destDeobfuscator);
	}
}
