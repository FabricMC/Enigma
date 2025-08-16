package cuchaz.enigma.source.jadx;

import java.util.Collection;

import jadx.api.data.CommentStyle;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxPreparePass;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;

public class JadxJavadocProvider implements JadxPlugin {
	public static final String PLUGIN_ID = "enigma-javadoc-provider";

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(PLUGIN_ID, "Enigma Javadoc Provider", "Applies Enigma-supplied Javadocs");
	}

	@SuppressWarnings("resource")
	@Override
	public void init(JadxPluginContext context) {
		CustomJadxArgs args = (CustomJadxArgs) context.getArgs();

		context.addPass(new JavadocProvidingPass(args.mapper, args.jadxHelper));
	}

	private static class JavadocProvidingPass implements JadxPreparePass {
		private final EntryRemapper mapper;
		private final JadxHelper jadxHelper;

		private JavadocProvidingPass(EntryRemapper mapper, JadxHelper jadxHelper) {
			this.mapper = mapper;
			this.jadxHelper = jadxHelper;
		}

		@Override
		public JadxPassInfo getInfo() {
			return new OrderedJadxPassInfo("ApplyJavadocs", "Applies Enigma-supplied Javadocs")
					.before("RenameVisitor");
		}

		@Override
		public void init(RootNode root) {
			process(root);
			root.registerCodeDataUpdateListener(codeData -> process(root));
		}

		private void process(RootNode root) {
			if (mapper == null) return;

			for (ClassNode cls : root.getClasses()) {
				processClass(cls);
			}
		}

		private void processClass(ClassNode cls) {
			EntryMapping mapping = mapper.getDeobfMapping(jadxHelper.classEntryOf(cls));

			if (mapping.javadoc() != null && !mapping.javadoc().isBlank()) {
				// TODO: Once JADX supports records, add @param tags for components
				attachJavadoc(cls, mapping.javadoc());
			}

			for (FieldNode field : cls.getFields()) {
				processField(field);
			}

			for (MethodNode method : cls.getMethods()) {
				processMethod(method);
			}
		}

		private void processField(FieldNode field) {
			EntryMapping mapping = mapper.getDeobfMapping(jadxHelper.fieldEntryOf(field));

			if (mapping.javadoc() != null && !mapping.javadoc().isBlank()) {
				attachJavadoc(field, mapping.javadoc());
			}
		}

		private void processMethod(MethodNode method) {
			Entry<?> entry = jadxHelper.methodEntryOf(method);
			EntryMapping mapping = mapper.getDeobfMapping(entry);
			StringBuilder builder = new StringBuilder();
			String javadoc = mapping.javadoc();

			if (javadoc != null) {
				builder.append(javadoc);
			}

			Collection<Entry<?>> children = mapper.getObfChildren(entry);
			boolean addedLf = false;

			if (children != null && !children.isEmpty()) {
				for (Entry<?> child : children) {
					if (child instanceof LocalVariableEntry) {
						mapping = mapper.getDeobfMapping(child);
						javadoc = mapping.javadoc();

						if (javadoc != null) {
							if (!addedLf) {
								addedLf = true;
								builder.append('\n');
							}

							builder.append(String.format("\n@param %s %s", mapping.targetName(), javadoc));
						}
					}
				}
			}

			javadoc = builder.toString();

			if (!javadoc.isBlank()) {
				attachJavadoc(method, javadoc);
			}
		}

		private void attachJavadoc(NotificationAttrNode target, String javadoc) {
			target.addCodeComment(javadoc.trim(), CommentStyle.JAVADOC);
		}
	}
}
