package cuchaz.enigma.classhandle;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.bytecode.translators.SourceFixVisitor;
import cuchaz.enigma.events.ClassHandleListener;
import cuchaz.enigma.gui.DecompiledClassSource;
import cuchaz.enigma.source.*;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.Utils;
import org.objectweb.asm.tree.ClassNode;

import static cuchaz.enigma.utils.Utils.withLock;

public final class ClassHandleProvider {

	private final EnigmaProject project;

	private final ExecutorService pool = Executors.newWorkStealingPool();
	private DecompilerService ds;
	private Decompiler decompiler;

	private final Map<ClassEntry, Entry> handles = new HashMap<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	public ClassHandleProvider(EnigmaProject project, DecompilerService ds) {
		this.project = project;
		this.ds = ds;
		this.decompiler = createDecompiler();
	}

	@Nullable
	public ClassHandle openClass(ClassEntry entry) {
		if (!project.getJarIndex().getEntryIndex().hasClass(entry)) return null;

		return withLock(lock.writeLock(), () -> {
			Entry e = handles.computeIfAbsent(entry, entry1 -> new Entry(this, entry1));
			return e.createHandle();
		});
	}

	public void setDecompilerService(DecompilerService ds) {
		this.ds = ds;
		this.decompiler = createDecompiler();
		withLock(lock.readLock(), () -> {
			handles.values().forEach(Entry::invalidate);
		});
	}

	public DecompilerService getDecompilerService() {
		return ds;
	}

	private Decompiler createDecompiler() {
		return ds.create(name -> {
			ClassNode node = project.getClassCache().getClassNode(name);

			if (node == null) {
				return null;
			}

			ClassNode fixedNode = new ClassNode();
			node.accept(new SourceFixVisitor(Utils.ASM_VERSION, fixedNode, project.getJarIndex()));
			return fixedNode;
		}, new SourceSettings(true, true));
	}

	public void invalidateMapped() {
		withLock(lock.readLock(), () -> {
			handles.values().forEach(Entry::invalidateMapped);
		});
	}

	public void invalidateMapped(ClassEntry entry) {
		withLock(lock.readLock(), () -> {
			Entry e = handles.get(entry);
			if (e != null) {
				e.invalidateMapped();
			}
		});
	}

	public void invalidateJavadoc(ClassEntry entry) {
		withLock(lock.readLock(), () -> {
			Entry e = handles.get(entry);
			if (e != null) {
				e.invalidateJavadoc();
			}
		});
	}

	private void deleteEntry(Entry entry) {
		withLock(lock.writeLock(), () -> {
			handles.remove(entry.entry);
		});
	}

	public void destroy() {
		pool.shutdown();
		try {
			pool.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		withLock(lock.writeLock(), () -> {
			handles.values().forEach(Entry::destroy);
			handles.clear();
		});
	}

	private static final class Entry {

		private final ClassHandleProvider p;
		private final ClassEntry entry;
		private final List<ClassHandleImpl> handles = new ArrayList<>();
		private Source uncommentedSource;
		private DecompiledClassSource source;

		private final List<CompletableFuture<Source>> waitingUncommentedSources = Collections.synchronizedList(new ArrayList<>());
		private final List<CompletableFuture<DecompiledClassSource>> waitingSources = Collections.synchronizedList(new ArrayList<>());

		private final AtomicInteger decompileVersion = new AtomicInteger();
		private final AtomicInteger javadocVersion = new AtomicInteger();
		private final AtomicInteger indexVersion = new AtomicInteger();
		private final AtomicInteger mappedVersion = new AtomicInteger();

		private final ReadWriteLock lock = new ReentrantReadWriteLock();

		private Entry(ClassHandleProvider p, ClassEntry entry) {
			this.p = p;
			this.entry = entry;
			invalidate();
		}

		public ClassHandleImpl createHandle() {
			ClassHandleImpl handle = new ClassHandleImpl(this);
			withLock(lock.writeLock(), () -> {
				handles.add(handle);
			});
			return handle;
		}

		public void invalidate() {
			continueMapSource(continueIndexSource(continueInsertJavadoc(decompile())));
		}

		public void invalidateJavadoc() {
			continueMapSource(continueIndexSource(continueInsertJavadoc(CompletableFuture.completedFuture(uncommentedSource))));
		}

		public void invalidateMapped() {
			continueMapSource(CompletableFuture.completedFuture(source));
		}

		private CompletableFuture<Source> decompile() {
			int v = decompileVersion.incrementAndGet();
			return CompletableFuture.supplyAsync(() -> {
				if (decompileVersion.get() != v) return null;

				Source uncommentedSource = p.decompiler.getSource(entry.getFullName());
				Entry.this.uncommentedSource = uncommentedSource;
				withLock(lock.readLock(), () -> handles.forEach(h -> h.onUncommentedSourceChanged(uncommentedSource)));
				return uncommentedSource;
			}, p.pool);
		}

		private CompletableFuture<Source> continueInsertJavadoc(CompletableFuture<Source> f) {
			int v = javadocVersion.incrementAndGet();
			return f.thenApplyAsync(uncommentedSource -> {
				if (uncommentedSource == null || javadocVersion.get() != v) return null;

				Source source = uncommentedSource.addJavadocs(p.project.getMapper());
				withLock(lock.readLock(), () -> handles.forEach(h -> h.onDocsChanged(source)));
				return source;
			}, p.pool);
		}

		private CompletableFuture<DecompiledClassSource> continueIndexSource(CompletableFuture<Source> f) {
			int v = indexVersion.incrementAndGet();
			return f.thenApplyAsync(jdSource -> {
				if (jdSource == null || indexVersion.get() != v) return null;

				SourceIndex index = jdSource.index();
				index.resolveReferences(p.project.getMapper().getObfResolver());
				DecompiledClassSource source = new DecompiledClassSource(entry, index);
				Entry.this.source = source;
				return source;
			}, p.pool);
		}

		private void continueMapSource(CompletableFuture<DecompiledClassSource> f) {
			int v = mappedVersion.incrementAndGet();
			f.thenAcceptAsync(source -> {
				if (source == null || mappedVersion.get() != v) return;

				source.remapSource(p.project, p.project.getMapper().getDeobfuscator());
				withLock(lock.readLock(), () -> handles.forEach(h -> h.onMappedSourceChanged(source)));
			}, p.pool);
		}

		public void closeHandle(ClassHandleImpl classHandle) {
			classHandle.destroy();
			withLock(lock.writeLock(), () -> {
				handles.remove(classHandle);
				// TODO don't delete immediately, but cache for a bit
				if (handles.isEmpty()) {
					p.deleteEntry(this);
				}
			});
		}

		public void destroy() {
			withLock(lock.writeLock(), () -> {
				handles.forEach(ClassHandleImpl::destroy);
				handles.clear();
			});
		}

		public CompletableFuture<Source> getUncommentedSourceAsync() {
			if (uncommentedSource != null) {
				return CompletableFuture.completedFuture(uncommentedSource);
			} else {
				CompletableFuture<Source> f = new CompletableFuture<>();
				waitingUncommentedSources.add(f);
				return f;
			}
		}

		public CompletableFuture<DecompiledClassSource> getSourceAsync() {
			if (source != null) {
				return CompletableFuture.completedFuture(source);
			} else {
				CompletableFuture<DecompiledClassSource> f = new CompletableFuture<>();
				waitingSources.add(f);
				return f;
			}
		}

	}

	private static final class ClassHandleImpl implements ClassHandle {

		private final Entry entry;

		private boolean valid = true;

		private final Set<ClassHandleListener> listeners = new HashSet<>();

		private ClassHandleImpl(Entry entry) {
			this.entry = entry;
		}

		@Override
		public ClassEntry getRef() {
			checkValid();
			return entry.entry;
		}

		@Nullable
		@Override
		public ClassEntry getDeobfRef() {
			checkValid();
			// cache this?
			return entry.p.project.getMapper().deobfuscate(entry.entry);
		}

		@Override
		public CompletableFuture<DecompiledClassSource> getSource() {
			checkValid();
			return entry.getSourceAsync();
		}

		@Override
		public CompletableFuture<Source> getUncommentedSource() {
			checkValid();
			return entry.getUncommentedSourceAsync();
		}

		public void onUncommentedSourceChanged(Source source) {
			listeners.forEach(l -> l.onUncommentedSourceChanged(this, source));
		}

		public void onDocsChanged(Source source) {
			listeners.forEach(l -> l.onDocsChanged(this, source));
		}

		public void onMappedSourceChanged(DecompiledClassSource source) {
			listeners.forEach(l -> l.onMappedSourceChanged(this, source));
		}

		@Override
		public void addListener(ClassHandleListener listener) {
			listeners.add(listener);
		}

		@Override
		public void removeListener(ClassHandleListener listener) {
			listeners.remove(listener);
		}

		@Override
		public ClassHandle copy() {
			checkValid();
			return entry.createHandle();
		}

		@Override
		public void close() {
			if (valid) entry.closeHandle(this);
		}

		private void checkValid() {
			if (!valid) throw new IllegalStateException("Class handle no longer valid");
		}

		public void destroy() {
			listeners.forEach(l -> l.onDeleted(this));
			valid = false;
		}

	}

}
